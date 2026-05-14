

use crate::handle::{StdbHandle, StdbResult};
use crate::schema::{reducer, GrantSource, PunishmentCategory, PunishmentKind, ServerRole, ServerStatus};
use serde_json::{json, Value};
use uuid::Uuid;

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

    #[allow(clippy::too_many_arguments)]
    pub async fn punish_issue(
        &self,
        target: Uuid,
        target_name: &str,
        staff: Uuid,
        staff_name: &str,
        kind: PunishmentKind,
        reason: &str,
        proof: &str,
        duration_seconds: i64,
        silent: bool,
        clear_inventory: bool,
        server: &str,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::PUNISH_ISSUE,
                json!([
                    target.to_string(),
                    target_name,
                    staff.to_string(),
                    staff_name,
                    kind.wire(),
                    reason,
                    proof,
                    duration_seconds,
                    silent,
                    clear_inventory,
                    server,
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

    pub async fn punish_clear_history(
        &self,
        target: Uuid,
        staff: Uuid,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::PUNISH_CLEAR_HISTORY,
                json!([target.to_string(), staff.to_string()]),
            )
            .await
    }

    pub async fn template_upsert(
        &self,
        title: &str,
        category: PunishmentCategory,
        duration: &str,
        information: &str,
        seeded: bool,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::TEMPLATE_UPSERT,
                json!([title, category.wire(), duration, information, seeded]),
            )
            .await
    }

    pub async fn template_remove(&self, title: &str) -> StdbResult<Value> {
        self.handle
            .call_raw(reducer::TEMPLATE_REMOVE, json!([title]))
            .await
    }

    pub async fn blacklist_add(
        &self,
        target: Uuid,
        target_name: &str,
        staff: Uuid,
        staff_name: &str,
        reason: &str,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::BLACKLIST_ADD,
                json!([
                    target.to_string(),
                    target_name,
                    staff.to_string(),
                    staff_name,
                    reason,
                ]),
            )
            .await
    }

    pub async fn blacklist_revoke(
        &self,
        entry_id: u64,
        staff: Uuid,
        reason: &str,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::BLACKLIST_REVOKE,
                json!([entry_id, staff.to_string(), reason]),
            )
            .await
    }

    #[allow(clippy::too_many_arguments)]
    pub async fn rank_define(
        &self,
        id: &str,
        display_name: &str,
        color: &str,
        dye: &str,
        prefix: &str,
        suffix: &str,
        weight: i32,
        staff: bool,
        donator: bool,
        parent: &str,
        permissions_json: &str,
        chat_prefix: &str,
        chat_format: &str,
        tab_prefix: &str,
        tab_format: &str,
        nametag_prefix: &str,
        nametag_format: &str,
        seeded: bool,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::RANK_DEFINE,
                json!([
                    id, display_name, color, dye, prefix, suffix, weight,
                    staff, donator, parent, permissions_json,
                    chat_prefix, chat_format, tab_prefix, tab_format,
                    nametag_prefix, nametag_format, seeded,
                ]),
            )
            .await
    }

    pub async fn rank_remove(&self, id: &str) -> StdbResult<Value> {
        self.handle
            .call_raw(reducer::RANK_REMOVE, json!([id]))
            .await
    }

    #[allow(clippy::too_many_arguments)]
    pub async fn grant_issue(
        &self,
        target: Uuid,
        target_name: &str,
        rank_id: &str,
        executor: Uuid,
        executor_name: &str,
        reason: &str,
        source: GrantSource,
        duration_seconds: i64,
    ) -> StdbResult<Value> {
        self.handle
            .call_raw(
                reducer::GRANT_ISSUE,
                json!([
                    target.to_string(),
                    target_name,
                    rank_id,
                    executor.to_string(),
                    executor_name,
                    reason,
                    source.wire(),
                    duration_seconds,
                ]),
            )
            .await
    }

    pub async fn grant_deactivate(&self, grant_id: u64) -> StdbResult<Value> {
        self.handle
            .call_raw(reducer::GRANT_DEACTIVATE, json!([grant_id]))
            .await
    }

    pub async fn grant_remove_inactive(&self, grant_id: u64) -> StdbResult<Value> {
        self.handle
            .call_raw(reducer::GRANT_REMOVE_INACTIVE, json!([grant_id]))
            .await
    }

    pub async fn grant_clear(&self, target: Uuid) -> StdbResult<Value> {
        self.handle
            .call_raw(reducer::GRANT_CLEAR, json!([target.to_string()]))
            .await
    }

    pub async fn grant_expire(&self) -> StdbResult<Value> {
        self.handle.call_raw(reducer::GRANT_EXPIRE, json!([])).await
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn enum_wire_values() {
        assert_eq!(ServerRole::Hub.wire(), "HUB");
        assert_eq!(ServerRole::Skyblock.wire(), "SKYBLOCK");
        assert_eq!(ServerStatus::Healthy.wire(), "HEALTHY");
        assert_eq!(ServerStatus::Draining.wire(), "DRAINING");
        assert_eq!(PunishmentKind::Ban.wire(), "BAN");
        assert_eq!(PunishmentKind::TempBan.wire(), "TEMP_BAN");
        assert_eq!(PunishmentKind::TempMute.wire(), "TEMP_MUTE");
        assert_eq!(PunishmentKind::Blacklist.wire(), "BLACKLIST");
        assert_eq!(PunishmentCategory::Ban.wire(), "BAN");
        assert_eq!(GrantSource::Staff.wire(), "STAFF");
        assert_eq!(GrantSource::Purchase.wire(), "PURCHASE");
    }

    #[test]
    fn grant_issue_args_shape() {
        let args = json!([
            "11111111-1111-1111-1111-111111111111",
            "Notch",
            "vip",
            "22222222-2222-2222-2222-222222222222",
            "Staff",
            "purchased rank",
            GrantSource::Purchase.wire(),
            0i64,
        ]);
        let arr = args.as_array().unwrap();
        assert_eq!(arr.len(), 8);
        assert_eq!(arr[2].as_str(), Some("vip"));
        assert_eq!(arr[6].as_str(), Some("PURCHASE"));
        assert_eq!(arr[7].as_i64(), Some(0));
    }

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
