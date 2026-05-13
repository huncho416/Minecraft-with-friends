//! Public client-facing handle. Send commands to the driver, await responses.

use serde_json::Value;
use thiserror::Error;
use tokio::sync::{mpsc, oneshot};
use uuid::Uuid;

/// Result type for every reducer/subscription call.
pub type StdbResult<T> = Result<T, StdbError>;

#[derive(Debug, Error)]
pub enum StdbError {
    #[error("driver task is gone (proxy shutting down?)")]
    DriverGone,
    #[error("driver dropped the response — likely socket closed mid-call")]
    ResponseDropped,
    #[error("STDB rejected reducer {reducer}: {message}")]
    ReducerRejected { reducer: String, message: String },
    #[error("subscription {table} failed: {message}")]
    SubscriptionFailed { table: String, message: String },
    #[error("schema mismatch: bridge expects v{expected}, module reports v{actual}")]
    SchemaMismatch { expected: u32, actual: u32 },
    #[error("schema version probe timed out")]
    SchemaProbeTimeout,
    #[error("json: {0}")]
    Json(#[from] serde_json::Error),
}

/// One command in flight to the driver.
pub(crate) enum Command {
    /// Call a reducer with positional args. Driver replies with raw payload
    /// or rejection message.
    CallReducer {
        request_id: Uuid,
        reducer: &'static str,
        args: Value,
        reply: oneshot::Sender<StdbResult<Value>>,
    },
    /// Subscribe to a table. Each row delivery is forwarded on `events`.
    Subscribe {
        table: &'static str,
        events: mpsc::UnboundedSender<TableEvent>,
        reply: oneshot::Sender<StdbResult<()>>,
    },
    /// Cooperative shutdown.
    Shutdown,
}

/// Row event forwarded from the driver to subscribers.
#[derive(Debug, Clone)]
pub struct TableEvent {
    pub table: &'static str,
    pub op: TableOp,
    /// Raw row payload — deserialize to your DTO.
    pub payload: String,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TableOp {
    Insert,
    Update,
    Delete,
}

/// Clone-able handle. Anything in the proxy that talks to STDB holds one
/// of these — the driver task is the singleton on the other end.
#[derive(Clone)]
pub struct StdbHandle {
    pub(crate) tx: mpsc::Sender<Command>,
}

impl StdbHandle {
    /// Call a reducer with `args` already serialized to a JSON value.
    /// Most callers use [`crate::client::MythicStdbClient`] instead.
    pub async fn call_raw(
        &self,
        reducer: &'static str,
        args: Value,
    ) -> StdbResult<Value> {
        let (reply_tx, reply_rx) = oneshot::channel();
        self.tx
            .send(Command::CallReducer {
                request_id: Uuid::new_v4(),
                reducer,
                args,
                reply: reply_tx,
            })
            .await
            .map_err(|_| StdbError::DriverGone)?;
        reply_rx.await.map_err(|_| StdbError::ResponseDropped)?
    }

    /// Subscribe to a table. Returns once the subscription is confirmed by
    /// STDB; row events flow on the returned receiver.
    pub async fn subscribe(
        &self,
        table: &'static str,
    ) -> StdbResult<mpsc::UnboundedReceiver<TableEvent>> {
        let (events_tx, events_rx) = mpsc::unbounded_channel();
        let (reply_tx, reply_rx) = oneshot::channel();
        self.tx
            .send(Command::Subscribe {
                table,
                events: events_tx,
                reply: reply_tx,
            })
            .await
            .map_err(|_| StdbError::DriverGone)?;
        reply_rx.await.map_err(|_| StdbError::ResponseDropped)??;
        Ok(events_rx)
    }

    /// Best-effort cooperative shutdown. Idempotent.
    pub async fn shutdown(&self) {
        let _ = self.tx.send(Command::Shutdown).await;
    }
}
