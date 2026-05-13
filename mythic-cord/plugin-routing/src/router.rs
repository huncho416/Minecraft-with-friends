//! Pure shard-selection logic. Mirror of `mythic_stdb::sessions::pick_shard`.
//!
//! Kept here (not in the bridge) because the bridge has no opinions about
//! routing — only this crate consumes [`registry_view::ServerEntry`].

use crate::registry_view::ServerEntry;
use mythiccord_stdb_bridge::ServerStatus;

/// Routing-friendly load score. Lower is better. Identical formula to
/// `mythic_stdb::registry::load_score` so client-side routing matches
/// what STDB-side reducers would compute.
///
/// `u32 → f32` is intentional: routing only needs the relative ordering,
/// and our `max_players` caps are <100k where f32 is exact.
#[allow(clippy::cast_precision_loss)]
pub fn load_score(e: &ServerEntry) -> f32 {
    let cap = e.max_players.max(1) as f32;
    let saturation = (e.player_count as f32) / cap;
    let tps_penalty = (20.0 - e.tps.min(20.0)) / 20.0;
    let heap_penalty = e.heap_load.clamp(0.0, 1.0);
    saturation + 0.5 * tps_penalty + 0.3 * heap_penalty
}

/// Pick the best target shard. Lowest [`load_score`] wins; ties broken
/// by region match, then by shard id for stability.
pub fn pick_shard<'a>(
    entries: &'a [ServerEntry],
    desired_role: &str,
    preferred_region: &str,
) -> Option<&'a ServerEntry> {
    entries
        .iter()
        .filter(|e| e.role == desired_role)
        .filter(|e| e.status == ServerStatus::Healthy.wire())
        .filter(|e| e.player_count < e.max_players)
        .min_by(|a, b| {
            let region_a = u32::from(a.region != preferred_region);
            let region_b = u32::from(b.region != preferred_region);
            load_score(a)
                .partial_cmp(&load_score(b))
                .unwrap_or(std::cmp::Ordering::Equal)
                .then(region_a.cmp(&region_b))
                .then(a.shard_id.cmp(&b.shard_id))
        })
}

#[cfg(test)]
mod tests {
    use super::*;

    // Test fixture builder — taking every field is clearer than a builder
    // here. Suppress the lint locally.
    #[allow(clippy::too_many_arguments)]
    fn entry(
        shard: &str,
        role: &str,
        region: &str,
        status: ServerStatus,
        players: u32,
        max: u32,
        tps: f32,
        heap: f32,
    ) -> ServerEntry {
        ServerEntry {
            shard_id: shard.into(),
            role: role.into(),
            region: region.into(),
            status: status.wire().into(),
            address: format!("{shard}:25565"),
            max_players: max,
            player_count: players,
            tps,
            heap_load: heap,
            schema_version: 1,
            started_at: 0,
            last_heartbeat: 0,
        }
    }

    #[test]
    fn skips_full_or_unhealthy_or_wrong_role() {
        let entries = vec![
            entry("a", "SKYBLOCK", "us-east", ServerStatus::Healthy, 100, 100, 20.0, 0.5),
            entry("b", "SKYBLOCK", "us-east", ServerStatus::Draining, 10, 100, 20.0, 0.5),
            entry("c", "HUB", "us-east", ServerStatus::Healthy, 10, 100, 20.0, 0.5),
        ];
        assert!(pick_shard(&entries, "SKYBLOCK", "us-east").is_none());
        assert_eq!(pick_shard(&entries, "HUB", "us-east").unwrap().shard_id, "c");
    }

    #[test]
    fn prefers_lower_load_then_region() {
        let entries = vec![
            entry("a", "SKYBLOCK", "us-east", ServerStatus::Healthy, 80, 100, 20.0, 0.5),
            entry("b", "SKYBLOCK", "eu-west", ServerStatus::Healthy, 20, 100, 20.0, 0.5),
            entry("c", "SKYBLOCK", "us-east", ServerStatus::Healthy, 30, 100, 20.0, 0.5),
        ];
        // b has lowest load, but c matches region with similar load.
        // Load wins first: b is picked.
        assert_eq!(pick_shard(&entries, "SKYBLOCK", "us-east").unwrap().shard_id, "b");
    }

    #[test]
    fn ties_broken_by_region_then_id() {
        let entries = vec![
            entry("z", "SKYBLOCK", "eu-west", ServerStatus::Healthy, 50, 100, 20.0, 0.5),
            entry("a", "SKYBLOCK", "eu-west", ServerStatus::Healthy, 50, 100, 20.0, 0.5),
            entry("m", "SKYBLOCK", "us-east", ServerStatus::Healthy, 50, 100, 20.0, 0.5),
        ];
        // Same load score; region match wins → "m".
        assert_eq!(pick_shard(&entries, "SKYBLOCK", "us-east").unwrap().shard_id, "m");
        // Without region match, smallest id wins → "a".
        assert_eq!(pick_shard(&entries, "SKYBLOCK", "ap-south").unwrap().shard_id, "a");
    }
}
