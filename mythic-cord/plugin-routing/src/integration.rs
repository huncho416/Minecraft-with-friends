//! Infrarust glue. Compiles only with `--features with-infrarust`.
//!
//! Subscribe to:
//! * `PreLoginEvent`  → call `session_login`, decide target shard, set the
//!   chosen backend on the event.
//! * `DisconnectEvent` (or limbo `on_disconnect`) → `session_logout`.
//!
//! Reading the survey's notes carefully: the exact API surface here is a
//! working sketch against `infrarust-api` v2.0.0-alpha.6. After the
//! vendor script lands the upstream subtree, we'll pin to a tagged commit
//! and adjust types to match the real signatures (alpha churn is real).

#![cfg(feature = "with-infrarust")]

use crate::{registry_view, router, RoutingRuntime};
use std::sync::Arc;
use tracing::{info, warn};
use uuid::Uuid;

/// Plugin entry point. Registers all event subscribers on the bus.
///
/// Call from your Infrarust plugin's `Plugin::on_load`. The exact bus
/// type comes from `infrarust_api`; we accept it as a generic so future
/// upstream renames don't force a cascade of edits here.
pub async fn install<B>(bus: &B, runtime: RoutingRuntime)
where
    B: infrarust_api::events::EventBus + Send + Sync,
{
    use infrarust_api::events::lifecycle::PreLoginEvent;

    let runtime_for_login = runtime.clone();
    bus.subscribe_async::<PreLoginEvent, _>(move |event| {
        let runtime = runtime_for_login.clone();
        async move {
            on_pre_login(event, &runtime).await;
        }
    })
    .await;

    // `DisconnectEvent` lives in the same module per the survey; if upstream
    // moves it we adjust the import.
    let runtime_for_logout = runtime.clone();
    bus.subscribe_async::<infrarust_api::events::lifecycle::DisconnectEvent, _>(move |event| {
        let runtime = runtime_for_logout.clone();
        async move {
            on_disconnect(event, &runtime).await;
        }
    })
    .await;

    // Spawn the registry mirror and the heartbeat task. They live until
    // the runtime is dropped at proxy shutdown.
    let view_clone = runtime.registry.clone();
    registry_view::spawn(runtime.stdb.handle().clone(), view_clone);
    tokio::spawn(crate::heartbeat::run(runtime));
}

async fn on_pre_login(
    event: infrarust_api::events::lifecycle::PreLoginEvent,
    runtime: &RoutingRuntime,
) {
    use mythiccord_stdb_bridge::ServerStatus;

    // Refuse new logins while draining — Pterodactyl will kill us soon.
    if matches!(*runtime.status.read(), ServerStatus::Draining | ServerStatus::Offline) {
        event.disconnect("Server draining; please reconnect in a moment.");
        return;
    }

    let uuid = match Uuid::parse_str(&event.player_uuid()) {
        Ok(u) => u,
        Err(_) => {
            warn!(uuid = %event.player_uuid(), "pre_login with bad UUID; rejecting");
            event.disconnect("Bad UUID");
            return;
        }
    };

    // Pick the destination shard before we commit a session row, so a
    // routing miss doesn't leave a ghost session.
    let snapshot = runtime.registry.snapshot();
    let target = router::pick_shard(
        &snapshot,
        runtime.identity.role.wire(),
        &runtime.identity.region,
    );
    let Some(target) = target else {
        event.disconnect("No available servers right now.");
        return;
    };

    let region = runtime.identity.region.clone();
    let shard = target.shard_id.clone();
    if let Err(e) = runtime
        .stdb
        .session_login(
            uuid,
            &event.username(),
            &shard,
            event.session_id(),
            &event.ip_hash(),
            &region,
        )
        .await
    {
        warn!(player = %event.username(), %e, "session_login failed; routing anyway");
    }

    event.set_backend(&target.address);
    info!(
        player = %event.username(),
        target = %shard,
        "routed",
    );
}

async fn on_disconnect(
    event: infrarust_api::events::lifecycle::DisconnectEvent,
    runtime: &RoutingRuntime,
) {
    if let Ok(uuid) = Uuid::parse_str(&event.player_uuid()) {
        let _ = runtime
            .stdb
            .session_logout(uuid, &event.reason())
            .await;
    }
}

/// Cooperative drain. Flip status, tell STDB, then return — the proxy's
/// signal handler waits for in-flight sessions to finish before exit.
pub async fn drain(runtime: &RoutingRuntime) {
    use mythiccord_stdb_bridge::ServerStatus;
    {
        let mut s = runtime.status.write();
        *s = ServerStatus::Draining;
    }
    crate::heartbeat::announce_drain(&runtime.identity, runtime).await;
}

/// Just bind the dependencies so the crate-graph check sees them used
/// even when `with-infrarust` is off but downstream forgets the feature.
#[doc(hidden)]
#[allow(dead_code)]
pub fn _link_check() {
    let _ = Arc::new(());
}
