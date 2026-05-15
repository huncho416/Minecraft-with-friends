

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
    let url = config
        .stdb_uri
        .replacen("http://", "ws://", 1)
        .replacen("https://", "wss://", 1);
    let url = format!("{url}/v1/database/{}/subscribe", config.module_name);
    let (ws, _) = tokio_tungstenite::connect_async(&url)
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
            "type": "subscribe",
            "queryStrings": [format!("SELECT * FROM {table}")],
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
                "type": "call",
                "requestId": request_id.to_string(),
                "reducer": reducer,
                "args": args,
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
                "type": "subscribe",
                "queryStrings": [format!("SELECT * FROM {table}")],
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

    if let Some(req_id) = obj.get("requestId").and_then(Value::as_str) {
        let id = Uuid::parse_str(req_id).map_err(|e| format!("uuid: {e}"))?;
        if let Some(reply) = state.pending_calls.remove(&id) {
            let result = if let Some(err) = obj.get("error").and_then(Value::as_str) {
                Err(StdbError::ReducerRejected {
                    reducer: obj
                        .get("reducer")
                        .and_then(Value::as_str)
                        .unwrap_or("?")
                        .into(),
                    message: err.into(),
                })
            } else {
                Ok(obj.get("payload").cloned().unwrap_or(Value::Null))
            };
            let _ = reply.send(result);
        }
        return Ok(());
    }

    if let Some(table) = obj.get("table").and_then(Value::as_str) {
        let op = match obj.get("operation").and_then(Value::as_str) {
            Some("insert") => TableOp::Insert,
            Some("delete") => TableOp::Delete,
            _ => TableOp::Update,
        };
        let payload = obj
            .get("payload")
            .map(ToString::to_string)
            .unwrap_or_default();

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
    Ok(())
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
