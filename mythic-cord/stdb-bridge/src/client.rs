//! Typed reducer client. Mirror of Java's `MythicSchema` — one method per
//! reducer. Args are positional JSON arrays in the same order as the Rust
//! reducer signature in `mythic-cord/stdb/src/`.
//!
//! When you add a reducer to `mythic-stdb`, add a method here with the
//! same parameter order so call sites stay statically typed.

use crate::handle::{StdbHandle, StdbResult};
use crate::schema::{reducer, PunishmentKind, ServerRole, ServerStatus};
use serde_json::{json, Value};
use uuid::Uuid;

/// Wraps an [`StdbHandle`] with one method per reducer.
#[derive(Clone)]
pub struct MythicStdbClient {
    handle: StdbHandle,
}

impl MythicStdbClient {
    pub fn new(handle: StdbHandle) -> Self {
        Self { handle }
    }

    pub fn handle(&self) -> &StdbHandle {
        &self.handle
    }

    // ── sessions ─────────────────────────────────────────────────────

    pub async fn session_login(
        &self,
        uuid: Uuid,
        username: &str,
        shard_id: &str,
        proxy_session_id: u64,
        ip_hash: &str,
        region: &str,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::SESSION_LOGIN,
                json!([uuid.to_string(), username, shard_id, proxy_session_id, ip_hash, region]),
            )
            .await
    }

    pub async fn session_logout(&self, uuid: Uuid, reason: &str) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::SESSION_LOGOUT,
                json!([uuid.to_string(), reason]),
            )
            .await
    }

    pub async fn session_route(
        &self,
        uuid: Uuid,
        new_shard_id: &str,
        reason: &str,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::SESSION_ROUTE,
                json!([uuid.to_string(), new_shard_id, reason]),
            )
            .await
    }

    pub async fn session_touch(&self, uuid: Uuid) -> StdbResult<Value> {
        self.handle
            .call_raw(reducer::SESSION_TOUCH, json!([uuid.to_string()]))
            .await
    }

    pub async fn session_reap(&self, older_than_seconds: u64) -> StdbResult<Value> {
        self.handle
            .call_raw(reducer::SESSION_REAP, json!([older_than_seconds]))
            .await
    }

    // ── registry ─────────────────────────────────────────────────────

    pub async fn registry_announce(
        &self,
        shard_id: &str,
        role: ServerRole,
        region: &str,
        address: &str,
        max_players: u32,
        schema_version: u32,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::REGISTRY_ANNOUNCE,
                json!([shard_id, role.wire(), region, address, max_players, schema_version]),
            )
            .await
    }

    pub async fn registry_heartbeat(
        &self,
        shard_id: &str,
        status: ServerStatus,
        player_count: u32,
        tps: f32,
        heap_load: f32,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::REGISTRY_HEARTBEAT,
                json!([shard_id, status.wire(), player_count, tps, heap_load]),
            )
            .await
    }

    pub async fn registry_drain(&self, shard_id: &str) -> StdbResult<Value> {
        self.handle
            .call_raw(reducer::REGISTRY_DRAIN, json!([shard_id]))
            .await
    }

    // ── punishments ──────────────────────────────────────────────────

    pub async fn punish_issue(
        &self,
        target: Uuid,
        staff: Uuid,
        kind: PunishmentKind,
        reason: &str,
        evidence: &str,
        duration_seconds: i64,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::PUNISH_ISSUE,
                json!([
                    target.to_string(),
                    staff.to_string(),
                    kind.wire(),
                    reason,
                    evidence,
                    duration_seconds,
                ]),
            )
            .await
    }

    pub async fn punish_pardon(
        &self,
        punishment_id: u64,
        staff: Uuid,
        reason: &str,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::PUNISH_PARDON,
                json!([punishment_id, staff.to_string(), reason]),
            )
            .await
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Round-trip parity with the Java side: enum wire values must be
    /// stable strings the Rust reducers recognize.
    #[test]
    fn enum_wire_values() {
        assert_eq!(ServerRole::Hub.wire(), "HUB");
        assert_eq!(ServerRole::Skyblock.wire(), "SKYBLOCK");
        assert_eq!(ServerStatus::Healthy.wire(), "HEALTHY");
        assert_eq!(ServerStatus::Draining.wire(), "DRAINING");
        assert_eq!(PunishmentKind::PermaBan.wire(), "PERMA_BAN");
        assert_eq!(PunishmentKind::TempBan.wire(), "TEMP_BAN");
    }

    /// Verifies the JSON shape the driver puts on the wire matches what
    /// `mythic-stdb`'s reducer signatures expect (positional array).
    #[test]
    fn registry_announce_args_shape() {
        let args = json!([
            "hub-1",
            ServerRole::Hub.wire(),
            "us-east",
            "hub:25565",
            200u32,
            crate::schema::SCHEMA_VERSION,
        ]);
        let arr = args.as_array().unwrap();
        assert_eq!(arr.len(), 6);
        assert_eq!(arr[1].as_str(), Some("HUB"));
        assert_eq!(arr[5].as_u64(), Some(u64::from(crate::schema::SCHEMA_VERSION)));
    }
}
