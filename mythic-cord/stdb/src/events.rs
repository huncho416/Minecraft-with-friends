use crate::common::{require_backend, PlayerUuid, ReducerResult, ShardId};
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = staff_chat_events, public)]
pub struct StaffChatEvent {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    pub channel: String,

    pub sender_uuid: PlayerUuid,
    pub sender_name: String,

    pub sender_rank: String,
    pub sender_rank_color: String,
    pub sender_chat_prefix: String,

    pub origin_shard: ShardId,

    pub message: String,

    pub created_at: Timestamp,
}

#[reducer]
#[allow(clippy::too_many_arguments)]
pub fn staff_chat_send(
    ctx: &ReducerContext,
    channel: String,
    sender_uuid: PlayerUuid,
    sender_name: String,
    sender_rank: String,
    sender_rank_color: String,
    sender_chat_prefix: String,
    origin_shard: ShardId,
    message: String,
) -> ReducerResult {
    require_backend(ctx)?;
    ctx.db.staff_chat_events().insert(StaffChatEvent {
        id: 0,
        channel,
        sender_uuid,
        sender_name,
        sender_rank,
        sender_rank_color,
        sender_chat_prefix,
        origin_shard,
        message,
        created_at: ctx.timestamp,
    });
    Ok(())
}

#[reducer]
pub fn staff_chat_prune(ctx: &ReducerContext, older_than_micros: i64) -> ReducerResult {
    require_backend(ctx)?;
    let cutoff = ctx.timestamp.to_micros_since_unix_epoch() - older_than_micros;
    let to_delete: Vec<u64> = ctx
        .db
        .staff_chat_events()
        .iter()
        .filter(|e| e.created_at.to_micros_since_unix_epoch() < cutoff)
        .map(|e| e.id)
        .collect();
    for id in to_delete {
        ctx.db.staff_chat_events().id().delete(id);
    }
    Ok(())
}

#[table(name = transfer_requests, public)]
pub struct TransferRequest {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub target_uuid: PlayerUuid,

    pub target_name: String,

    pub destination_shard: ShardId,

    pub requester_uuid: PlayerUuid,
    pub requester_name: String,

    pub created_at: Timestamp,
}

#[reducer]
pub fn transfer_request_create(
    ctx: &ReducerContext,
    target_uuid: PlayerUuid,
    target_name: String,
    destination_shard: ShardId,
    requester_uuid: PlayerUuid,
    requester_name: String,
) -> ReducerResult {
    require_backend(ctx)?;
    ctx.db.transfer_requests().insert(TransferRequest {
        id: 0,
        target_uuid,
        target_name,
        destination_shard,
        requester_uuid,
        requester_name,
        created_at: ctx.timestamp,
    });
    Ok(())
}

#[reducer]
pub fn transfer_request_complete(ctx: &ReducerContext, request_id: u64) -> ReducerResult {
    require_backend(ctx)?;
    ctx.db.transfer_requests().id().delete(request_id);
    Ok(())
}

#[reducer]
pub fn transfer_request_prune(ctx: &ReducerContext, older_than_micros: i64) -> ReducerResult {
    require_backend(ctx)?;
    let cutoff = ctx.timestamp.to_micros_since_unix_epoch() - older_than_micros;
    let to_delete: Vec<u64> = ctx
        .db
        .transfer_requests()
        .iter()
        .filter(|r| r.created_at.to_micros_since_unix_epoch() < cutoff)
        .map(|r| r.id)
        .collect();
    for id in to_delete {
        ctx.db.transfer_requests().id().delete(id);
    }
    Ok(())
}

#[table(name = reports, public)]
pub struct ReportRow {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub reporter_uuid: PlayerUuid,
    pub reporter_name: String,

    #[index(btree)]
    pub target_uuid: PlayerUuid,
    pub target_name: String,

    pub category: String,

    pub reporter_shard: ShardId,

    pub created_at: Timestamp,

    #[index(btree)]
    pub resolved: bool,

    pub resolver_uuid: String,
    pub resolver_name: String,
    pub resolution: String,
    pub resolved_at_micros: i64,
}

#[reducer]
#[allow(clippy::too_many_arguments)]
pub fn report_create(
    ctx: &ReducerContext,
    reporter_uuid: PlayerUuid,
    reporter_name: String,
    target_uuid: PlayerUuid,
    target_name: String,
    category: String,
    reporter_shard: ShardId,
) -> ReducerResult {
    require_backend(ctx)?;
    ctx.db.reports().insert(ReportRow {
        id: 0,
        reporter_uuid,
        reporter_name,
        target_uuid,
        target_name,
        category,
        reporter_shard,
        created_at: ctx.timestamp,
        resolved: false,
        resolver_uuid: String::new(),
        resolver_name: String::new(),
        resolution: String::new(),
        resolved_at_micros: 0,
    });
    Ok(())
}

#[reducer]
pub fn report_resolve(
    ctx: &ReducerContext,
    report_id: u64,
    resolver_uuid: String,
    resolver_name: String,
    resolution: String,
) -> ReducerResult {
    require_backend(ctx)?;
    let reports = ctx.db.reports();
    let Some(mut row) = reports.id().find(report_id) else {
        return Ok(());
    };
    row.resolved = true;
    row.resolver_uuid = resolver_uuid;
    row.resolver_name = resolver_name;
    row.resolution = resolution;
    row.resolved_at_micros = ctx.timestamp.to_micros_since_unix_epoch();
    reports.id().update(row);
    Ok(())
}
