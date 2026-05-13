package net.mythicpvp.core.punishment;

import net.mythicpvp.suite.protocol.ProtocolManager;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class PunishmentService {

    public static final String CHANNEL = "core:punishment-update";
    private final ProtocolManager protocolManager;
    private final Clock clock;
    private final AtomicLong ids = new AtomicLong();
    private final List<PunishmentRecord> records = new CopyOnWriteArrayList<>();
    private final List<PunishmentNotice> notices = new CopyOnWriteArrayList<>();

    public PunishmentService(@NotNull ProtocolManager protocolManager, @NotNull Clock clock) {
        this.protocolManager = protocolManager;
        this.clock = clock;
        this.protocolManager.subscribe(CHANNEL, message -> receive(message.deserialize(PunishmentNotice.class)));
    }

    @NotNull
    public PunishmentRecord punish(@NotNull PunishmentRequest request) {
        Instant now = clock.instant();
        long expiresAtMillis = request.expiresAt() == null ? 0L : request.expiresAt().toEpochMilli();
        PunishmentRecord record = new PunishmentRecord(ids.incrementAndGet(), request.targetUuid(), request.targetName(), request.staffUuid(), request.staffName(), request.type(), request.reason(), now.toEpochMilli(), expiresAtMillis, request.silent(), false, request.server());
        records.add(record);
        protocolManager.publish(CHANNEL, new PunishmentNotice(record, !request.silent()));
        return record;
    }

    public boolean pardon(long id) {
        for (PunishmentRecord record : records) {
            if (record.id() == id && !record.pardoned()) {
                records.remove(record);
                records.add(new PunishmentRecord(record.id(), record.targetUuid(), record.targetName(), record.staffUuid(), record.staffName(), record.type(), record.reason(), record.createdAtMillis(), record.expiresAtMillis(), record.silent(), true, record.server()));
                return true;
            }
        }
        return false;
    }

    @NotNull
    public List<PunishmentRecord> history(@NotNull UUID targetUuid) {
        return records.stream()
                .filter(record -> record.targetUuid().equals(targetUuid))
                .sorted(Comparator.comparingLong(PunishmentRecord::createdAtMillis).reversed())
                .toList();
    }

    @NotNull
    public List<PunishmentRecord> active(@NotNull UUID targetUuid) {
        long nowMillis = clock.instant().toEpochMilli();
        return history(targetUuid).stream()
                .filter(record -> record.active(nowMillis))
                .toList();
    }

    @NotNull
    public List<PunishmentNotice> notices() {
        return List.copyOf(notices);
    }

    private void receive(@NotNull PunishmentNotice notice) {
        notices.add(notice);
    }
}
