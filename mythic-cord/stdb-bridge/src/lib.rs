//! MythicCord ↔ SpacetimeDB bridge.
//!
//! Architecture (lifted in spirit from SpacerCord's `infrarust-spacetimedb`,
//! rewritten against the `mythic-stdb` schema):
//!
//! ```text
//!   ┌─────────────────┐    Clone     ┌──────────────────────┐
//!   │ StdbHandle      │ ───────────► │ StdbHandle (consumer)│
//!   │ (Clone)         │              │ inside Infrarust     │
//!   └────────┬────────┘              │ plugins              │
//!            │ mpsc::Sender          └──────────────────────┘
//!            ▼
//!   ┌─────────────────────────────────────────┐
//!   │ stdb-driver task (single tokio task)    │
//!   │ owns the WebSocket, multiplexes calls,  │
//!   │ correlates responses by request_id      │
//!   └────────┬────────────────────────────────┘
//!            │ WebSocket
//!            ▼
//!         SpacetimeDB host
//! ```
//!
//! Why a single driver task instead of letting every plugin own its own
//! socket: WebSocket reads/writes aren't `Sync`; multiplexing through one
//! task lets us call from any number of `Clone`d handles without locks
//! around the socket itself. Bounded channels give us backpressure when
//! a slow STDB host falls behind.

// Project nouns ("MythicCord", "SpacetimeDB", "SpacerCord") show up in
// module docs; not Rust identifiers, so the doc-backticks lint is wrong.
#![allow(clippy::doc_markdown)]
#![allow(clippy::missing_const_for_fn)]

pub mod client;
pub mod driver;
pub mod handle;
pub mod schema;

pub use client::MythicStdbClient;
pub use driver::{spawn_driver, DriverConfig};
pub use handle::{StdbError, StdbHandle, StdbResult};
pub use schema::*;
