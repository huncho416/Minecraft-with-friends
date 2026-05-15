

use crate::handle::{Command, StdbError, StdbHandle, StdbResult, TableEvent, TableOp};
use crate::schema::SCHEMA_VERSION;
use futures_util::{SinkExt, StreamExt};
use serde_json::{json, Value};
use std::collections::HashMap;
use std::time::Duration;
use tokio::net::TcpStream;
use tokio::sync::{mpsc, oneshot};
use tokio_tungstenite::{tungstenite::Message, MaybeTlsStream, WebSocketStream};
use tracing::{debug, error, info, warn};
use uuid::Uuid;

#[derive(Debug, Clone)]
pub struct DriverConfig {

    pub stdb_uri: String,
    pub module_name: String,

    pub command_capacity: usize,

    pub reconnect_initial: Duration,
}

impl Default for DriverConfig {
    fn default() -> Self {
        Self {
            stdb_uri: "http://spacetimedb:3000".into(),
            module_name: "mythicpvp".into(),
            command_capacity: 1024,
            reconnect_initial: Duration::from_millis(500),
        }
    }
}

pub fn spawn_driver(config: DriverConfig) -> (StdbHandle, tokio::task::JoinHandle<()>) {
    let (tx, rx) = mpsc::channel(config.command_capacity);
    let handle = StdbHandle { tx };
    let join = tokio::spawn(driver_main(config, rx));
    (handle, join)
}

type WsStream = WebSocketStream<MaybeTlsStream<TcpStream>>;

struct InFlight {
    pending_calls: HashMap<Uuid, oneshot::Sender<StdbResult<Value>>>,
    subscriptions: HashMap<&'static str, Vec<mpsc::UnboundedSender<TableEvent>>>,
}

impl InFlight {
    fn new() -> Self {
        Self {
            pending_calls: HashMap::new(),
            subscriptions: HashMap::new(),
        }
    }

    fn fail_all_pending(&mut self) {
        for (_, tx) in self.pending_calls.drain() {
            let _ = tx.send(Err(StdbError::ResponseDropped));
        }
    }

    fn prune_subscribers(&mut self) {
        for senders in self.subscriptions.values_mut() {
            senders.retain(|s| !s.is_closed());
        }
        self.subscriptions.retain(|_, v| !v.is_empty());
    }
}

async fn driver_main(config: DriverConfig, mut rx: mpsc::Receiver<Command>) {
    let mut backoff = config.reconnect_initial;
    let mut state = InFlight::new();

    loop {
        match connect(&config).await {
            Ok(ws) => {
                info!("stdb-driver connected to {}", config.stdb_uri);
                backoff = config.reconnect_initial;
                if let Err(e) = run_session(&config, ws, &mut rx, &mut state).await {
                    warn!("stdb-driver session ended: {e}");
                }
                state.fail_all_pending();

                if rx.is_closed() {
                    info!("stdb-driver: command channel closed, exiting");
                    return;
                }
            }
            Err(e) => {
                error!("stdb-driver connect failed: {e}; retrying in {:?}", backoff);
            }
        }
        tokio::time::sleep(backoff).await;
        backoff = (backoff * 2).min(Duration::from_secs(30));
    }
}

async fn connect(config: &DriverConfig) -> Result<WsStream, String> {
    use tokio_tungstenite::tungstenite::client::IntoClientRequest;
    
    let url = config
        .stdb_uri
        .replacen("http://", "ws://", 1)
        .replacen("https://", "wss://", 1);
    let url = format!("{url}/v1/database/{}/subscribe", config.module_name);
    
    let mut req = url.clone().into_client_request().map_err(|e| format!("bad url: {e}"))?;
    req.headers_mut().insert(
        "Sec-WebSocket-Protocol",
        "v1.json.spacetimedb".parse().unwrap()
    );

    let (ws, _) = tokio_tungstenite::connect_async(req)
        .await
        .map_err(|e| format!("ws connect to {url}: {e}"))?;
    Ok(ws)
}

async fn run_session(
    _config: &DriverConfig,
    ws: WsStream,
    rx: &mut mpsc::Receiver<Command>,
    state: &mut InFlight,
) -> Result<(), String> {
    let (mut sink, mut stream) = ws.split();

    for table in state.subscriptions.keys().copied().collect::<Vec<_>>() {
        let msg = json!({
            "Subscribe": {
                "query_strings": [format!("SELECT * FROM {table}")],
                "request_id": 1
            }
        })
        .to_string();
        if let Err(e) = sink.send(Message::Text(msg)).await {
            return Err(format!("re-subscribe failed: {e}"));
        }
    }

    let mut prune_tick = tokio::time::interval(Duration::from_secs(30));

    loop {
        tokio::select! {
            biased;

            cmd = rx.recv() => {
                let Some(cmd) = cmd else {
                    debug!("command channel closed");
                    return Ok(());
                };
                handle_command(cmd, &mut sink, state).await?;
            }

            msg = stream.next() => {
                match msg {
                    Some(Ok(Message::Text(text))) => {
                        if let Err(e) = handle_incoming(&text, state) {
                            warn!("malformed STDB message ignored: {e}");
                        }
                    }
                    Some(Ok(Message::Close(_))) | None => {
                        return Err("socket closed".into());
                    }
                    Some(Ok(Message::Ping(p))) => {
                        let _ = sink.send(Message::Pong(p)).await;
                    }

                    Some(Ok(_)) => {}
                    Some(Err(e)) => return Err(format!("ws read: {e}")),
                }
            }

            _ = prune_tick.tick() => {
                state.prune_subscribers();
            }
        }
    }
}

async fn handle_command(
    cmd: Command,
    sink: &mut futures_util::stream::SplitSink<WsStream, Message>,
    state: &mut InFlight,
) -> Result<(), String> {
    match cmd {
        Command::CallReducer { request_id, reducer, args, reply } => {
            let envelope = json!({
                "CallReducer": {
                    "reducer": reducer,
                    "args": args,
                    "request_id": request_id.to_string(),
                    "flags": 0
                }
            });
            if let Err(e) = sink.send(Message::Text(envelope.to_string())).await {
                let _ = reply.send(Err(StdbError::ResponseDropped));
                return Err(format!("send reducer: {e}"));
            }
            state.pending_calls.insert(request_id, reply);
            Ok(())
        }
        Command::Subscribe { table, events, reply } => {
            let envelope = json!({
                "Subscribe": {
                    "query_strings": [format!("SELECT * FROM {table}")],
                    "request_id": 1
                }
            });
            if let Err(e) = sink.send(Message::Text(envelope.to_string())).await {
                let _ = reply.send(Err(StdbError::SubscriptionFailed {
                    table: table.into(),
                    message: e.to_string(),
                }));
                return Err(format!("send subscribe: {e}"));
            }
            state.subscriptions.entry(table).or_default().push(events);

            let _ = reply.send(Ok(()));
            Ok(())
        }
        Command::Shutdown => Err("shutdown requested".into()),
    }
}

fn handle_incoming(text: &str, state: &mut InFlight) -> Result<(), String> {
    let value: Value = serde_json::from_str(text).map_err(|e| format!("json: {e}"))?;
    let obj = value.as_object().ok_or("expected object")?;

    // We can ignore IdentityToken
    if obj.contains_key("IdentityToken") {
        return Ok(());
    }

    // Handle v1 CallReducerResponse if present
    if let Some(call_res) = obj.get("CallReducerResponse") {
        if let Some(req_id) = call_res.get("request_id").and_then(Value::as_str) {
            let id = match Uuid::parse_str(req_id) {
                Ok(u) => u,
                Err(_) => return Ok(()),
            };
            if let Some(reply) = state.pending_calls.remove(&id) {
                let result = if let Some(err) = call_res.get("error").and_then(Value::as_str) {
                    Err(StdbError::ReducerRejected {
                        reducer: "?".into(),
                        message: err.into(),
                    })
                } else {
                    Ok(call_res.get("payload").cloned().unwrap_or(Value::Null))
                };
                let _ = reply.send(result);
            }
        }
        return Ok(());
    }

    // Handle v1 Subscription data
    let db_update = if let Some(init) = obj.get("InitialSubscription") {
        init.get("database_update")
    } else if let Some(update) = obj.get("SubscriptionUpdate") {
        update.get("database_update")
    } else {
        None
    };

    if let Some(db_update) = db_update {
        if let Some(tables) = db_update.get("tables").and_then(Value::as_array) {
            for table_obj in tables {
                if let Some(table_name) = table_obj.get("table_name").and_then(Value::as_str) {
                    if let Some(updates) = table_obj.get("updates").and_then(Value::as_array) {
                        for update in updates {
                            if let Some(inserts) = update.get("inserts").and_then(Value::as_array) {
                                for insert in inserts {
                                    dispatch_event(table_name, TableOp::Insert, insert.as_str().unwrap_or_default().to_string(), state);
                                }
                            }
                            if let Some(deletes) = update.get("deletes").and_then(Value::as_array) {
                                for delete in deletes {
                                    dispatch_event(table_name, TableOp::Delete, delete.as_str().unwrap_or_default().to_string(), state);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Ok(())
}

fn dispatch_event(table: &str, op: TableOp, payload: String, state: &mut InFlight) {
    let subs = state
        .subscriptions
        .iter()
        .find(|(k, _)| **k == table)
        .map(|(k, v)| (*k, v.clone()));
    if let Some((static_table, senders)) = subs {
        let event = TableEvent {
            table: static_table,
            op,
            payload,
        };
        for s in senders {
            let _ = s.send(event.clone());
        }
    }
}

pub async fn assert_schema_version(handle: &StdbHandle) -> StdbResult<()> {
    use crate::schema::table::MODULE_META;
    let mut events = handle.subscribe(MODULE_META).await?;
    let result = tokio::time::timeout(Duration::from_secs(10), async {
        while let Some(event) = events.recv().await {
            let Ok(parsed) = serde_json::from_str::<Value>(&event.payload) else {
                continue;
            };
            let Some(v) = parsed.get("schema_version").and_then(Value::as_u64) else {
                continue;
            };

            let actual = u32::try_from(v).unwrap_or(u32::MAX);
            if actual == SCHEMA_VERSION {
                return Ok(());
            }
            return Err(StdbError::SchemaMismatch {
                expected: SCHEMA_VERSION,
                actual,
            });
        }
        Err(StdbError::ResponseDropped)
    })
    .await;
    match result {
        Ok(inner) => inner,
        Err(_) => Err(StdbError::SchemaProbeTimeout),
    }
}
