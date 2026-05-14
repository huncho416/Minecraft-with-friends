

use serde_json::Value;
use thiserror::Error;
use tokio::sync::{mpsc, oneshot};
use uuid::Uuid;

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

pub(crate) enum Command {

    CallReducer {
        request_id: Uuid,
        reducer: &'static str,
        args: Value,
        reply: oneshot::Sender<StdbResult<Value>>,
    },

    Subscribe {
        table: &'static str,
        events: mpsc::UnboundedSender<TableEvent>,
        reply: oneshot::Sender<StdbResult<()>>,
    },

    Shutdown,
}

#[derive(Debug, Clone)]
pub struct TableEvent {
    pub table: &'static str,
    pub op: TableOp,

    pub payload: String,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TableOp {
    Insert,
    Update,
    Delete,
}

#[derive(Clone)]
pub struct StdbHandle {
    pub(crate) tx: mpsc::Sender<Command>,
}

impl StdbHandle {

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

    pub async fn shutdown(&self) {
        let _ = self.tx.send(Command::Shutdown).await;
    }
}
