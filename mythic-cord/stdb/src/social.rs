

use crate::common::{require_uuid, PlayerUuid, ReducerResult};
use crate::reject;
use spacetimedb::{reducer, table, ReducerContext, Table, Timestamp};

#[table(name = friends, public)]
pub struct Friend {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub owner_uuid: PlayerUuid,

    #[index(btree)]
    pub friend_uuid: PlayerUuid,

    pub added_at: Timestamp,
}

#[table(name = friend_requests, public)]
pub struct FriendRequest {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub from_uuid: PlayerUuid,

    #[index(btree)]
    pub to_uuid: PlayerUuid,

    pub created_at: Timestamp,
}

#[reducer]
pub fn friend_request(ctx: &ReducerContext, from: PlayerUuid, to: PlayerUuid) -> ReducerResult {
    require_uuid(&from)?;
    require_uuid(&to)?;
    if from == to {
        reject!("cannot friend yourself");
    }
    let exists = ctx
        .db
        .friend_requests()
        .iter()
        .any(|r| r.from_uuid == from && r.to_uuid == to);
    if exists {
        return Ok(());
    }
    ctx.db.friend_requests().insert(FriendRequest {
        id: 0,
        from_uuid: from,
        to_uuid: to,
        created_at: ctx.timestamp,
    });
    Ok(())
}

#[reducer]
pub fn friend_accept(ctx: &ReducerContext, request_id: u64) -> ReducerResult {
    let requests = ctx.db.friend_requests();
    let Some(r) = requests.id().find(request_id) else {
        reject!("request {request_id} not found");
    };
    let now = ctx.timestamp;
    let (a, b) = (r.from_uuid.clone(), r.to_uuid.clone());
    requests.id().delete(request_id);

    let friends = ctx.db.friends();
    friends.insert(Friend {
        id: 0,
        owner_uuid: a.clone(),
        friend_uuid: b.clone(),
        added_at: now,
    });
    friends.insert(Friend {
        id: 0,
        owner_uuid: b,
        friend_uuid: a,
        added_at: now,
    });
    Ok(())
}

#[reducer]
pub fn friend_deny(ctx: &ReducerContext, request_id: u64) -> ReducerResult {
    let requests = ctx.db.friend_requests();
    let Some(_r) = requests.id().find(request_id) else {
        reject!("request {request_id} not found");
    };
    requests.id().delete(request_id);
    Ok(())
}

#[reducer]
pub fn friend_remove(ctx: &ReducerContext, owner: PlayerUuid, friend: PlayerUuid) -> ReducerResult {
    require_uuid(&owner)?;
    require_uuid(&friend)?;
    let friends = ctx.db.friends();
    let to_delete: Vec<u64> = friends
        .iter()
        .filter(|f| {
            (f.owner_uuid == owner && f.friend_uuid == friend)
                || (f.owner_uuid == friend && f.friend_uuid == owner)
        })
        .map(|f| f.id)
        .collect();
    for id in to_delete {
        friends.id().delete(id);
    }
    Ok(())
}

#[table(name = parties, public)]
pub struct Party {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub leader_uuid: PlayerUuid,

    pub created_at: Timestamp,
}

#[table(name = party_members, public)]
pub struct PartyMember {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub party_id: u64,

    #[index(btree)]
    pub player_uuid: PlayerUuid,

    pub joined_at: Timestamp,
}

#[reducer]
pub fn party_create(ctx: &ReducerContext, leader: PlayerUuid) -> ReducerResult {
    require_uuid(&leader)?;

    let exists = ctx.db.parties().iter().any(|p| p.leader_uuid == leader);
    if exists {
        reject!("{leader} already leads a party");
    }
    let parties = ctx.db.parties();
    let inserted = parties.insert(Party {
        id: 0,
        leader_uuid: leader.clone(),
        created_at: ctx.timestamp,
    });
    ctx.db.party_members().insert(PartyMember {
        id: 0,
        party_id: inserted.id,
        player_uuid: leader,
        joined_at: ctx.timestamp,
    });
    Ok(())
}

#[reducer]
pub fn party_join(ctx: &ReducerContext, party_id: u64, player: PlayerUuid) -> ReducerResult {
    require_uuid(&player)?;
    if ctx.db.parties().id().find(party_id).is_none() {
        reject!("party {party_id} not found");
    }
    let members = ctx.db.party_members();
    let already = members
        .iter()
        .any(|m| m.party_id == party_id && m.player_uuid == player);
    if already {
        return Ok(());
    }
    members.insert(PartyMember {
        id: 0,
        party_id,
        player_uuid: player,
        joined_at: ctx.timestamp,
    });
    Ok(())
}

#[reducer]
pub fn party_leave(ctx: &ReducerContext, party_id: u64, player: PlayerUuid) -> ReducerResult {
    let members = ctx.db.party_members();
    let to_delete: Vec<u64> = members
        .iter()
        .filter(|m| m.party_id == party_id && m.player_uuid == player)
        .map(|m| m.id)
        .collect();
    for id in to_delete {
        members.id().delete(id);
    }

    if let Some(p) = ctx.db.parties().id().find(party_id) {
        if p.leader_uuid == player {
            party_disband(ctx, party_id)?;
        }
    }
    Ok(())
}

#[reducer]
pub fn party_disband(ctx: &ReducerContext, party_id: u64) -> ReducerResult {
    let members = ctx.db.party_members();
    let ids: Vec<u64> = members
        .iter()
        .filter(|m| m.party_id == party_id)
        .map(|m| m.id)
        .collect();
    for id in ids {
        members.id().delete(id);
    }
    ctx.db.parties().id().delete(party_id);
    Ok(())
}

#[table(name = mail, public)]
pub struct Mail {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[index(btree)]
    pub recipient_uuid: PlayerUuid,

    pub sender_uuid: PlayerUuid,
    pub subject: String,
    pub body: String,

    pub attachments_json: String,

    #[index(btree)]
    pub read: bool,

    pub sent_at: Timestamp,
    pub read_at_micros: i64,
}

#[reducer]
pub fn mail_send(
    ctx: &ReducerContext,
    sender: PlayerUuid,
    recipient: PlayerUuid,
    subject: String,
    body: String,
    attachments_json: String,
) -> ReducerResult {
    require_uuid(&sender)?;
    require_uuid(&recipient)?;
    ctx.db.mail().insert(Mail {
        id: 0,
        recipient_uuid: recipient,
        sender_uuid: sender,
        subject,
        body,
        attachments_json,
        read: false,
        sent_at: ctx.timestamp,
        read_at_micros: 0,
    });
    Ok(())
}

#[reducer]
pub fn mail_mark_read(ctx: &ReducerContext, mail_id: u64) -> ReducerResult {
    let mail = ctx.db.mail();
    let Some(mut m) = mail.id().find(mail_id) else {
        reject!("mail {mail_id} not found");
    };
    if !m.read {
        m.read = true;
        m.read_at_micros = ctx.timestamp.to_micros_since_unix_epoch();
        mail.id().update(m);
    }
    Ok(())
}

#[table(name = login_streaks, public)]
pub struct LoginStreakRow {
    #[primary_key]
    #[auto_inc]
    pub id: u64,

    #[unique]
    #[index(btree)]
    pub player_uuid: PlayerUuid,

    pub last_login_at: Timestamp,
    pub current_streak: i32,
}

#[reducer]
pub fn login_streak_record(
    ctx: &ReducerContext,
    player: PlayerUuid,
    login_millis: i64,
    streak: i32,
) -> ReducerResult {
    require_uuid(&player)?;
    let streaks = ctx.db.login_streaks();
    let existing: Option<LoginStreakRow> = streaks.iter().find(|s| s.player_uuid == player);
    if let Some(mut row) = existing {
        row.last_login_at = ctx.timestamp;
        row.current_streak = streak;
        streaks.id().update(row);
    } else {
        streaks.insert(LoginStreakRow {
            id: 0,
            player_uuid: player,
            last_login_at: ctx.timestamp,
            current_streak: streak,
        });
    }
    Ok(())
}
