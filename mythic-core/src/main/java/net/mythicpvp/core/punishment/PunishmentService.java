package net.mythicpvp.core.punishment;

import net.mythicpvp.core.persistence.NoopPersistenceGateway;
import net.mythicpvp.core.persistence.PersistenceGateway;
import net.mythicpvp.suite.protocol.ProtocolManager;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class PunishmentService {

    public static final String CHANNEL = "core:punishment-update";

    public static final UUID SYSTEM_STAFF = new UUID(0L, 0L);

    private final ProtocolManager protocolManager;
    private final Clock clock;
    private final AtomicLong ids = new AtomicLong();
    private final List<PunishmentRecord> records = new CopyOnWriteArrayList<>();
    private final List<PunishmentNotice> notices = new CopyOnWriteArrayList<>();
    private final List<PunishmentTemplate> templates = new CopyOnWriteArrayList<>();

    private volatile PersistenceGateway persistence = NoopPersistenceGateway.INSTANCE;
    private volatile Consumer<PunishmentNotice> enforcer = notice -> {};
    private volatile Consumer<PunishmentRecord> pardonListener = record -> {};
    private volatile Consumer<PardonNotice> pardonNoticeListener = notice -> {};
    private volatile Consumer<PunishmentRecord> expiryListener = record -> {};

    public PunishmentService(@NotNull ProtocolManager protocolManager, @NotNull Clock clock) {
        this.protocolManager = protocolManager;
        this.clock = clock;
        this.protocolManager.subscribe(CHANNEL, message -> receive(message.deserialize(PunishmentNotice.class)));
    }

    public void setPersistence(@NotNull PersistenceGateway persistence) {
        this.persistence = persistence;
    }

    public void setEnforcer(@NotNull Consumer<PunishmentNotice> enforcer) {
        this.enforcer = enforcer;
    }

    public void setPardonListener(@NotNull Consumer<PunishmentRecord> listener) {
        this.pardonListener = listener;
    }

    public void setPardonNoticeListener(@NotNull Consumer<PardonNotice> listener) {
        this.pardonNoticeListener = listener;
    }

    public int pardonActive(@NotNull UUID targetUuid,
                            @NotNull Set<PunishmentType> types,
                            @NotNull UUID staff,
                            @NotNull String staffName,
                            @NotNull String reason,
                            boolean silent) {
        int pardoned = 0;
        PunishmentRecord firstPardoned = null;
        long nowMillis = clock.instant().toEpochMilli();
        for (PunishmentRecord record : List.copyOf(records)) {
            if (record.targetUuid().equals(targetUuid)
                    && types.contains(record.type())
                    && record.active(nowMillis)
                    && pardon(record.id(), staff, reason)) {
                pardoned++;
                if (firstPardoned == null) {
                    firstPardoned = records.stream()
                            .filter(r -> r.id() == record.id())
                            .findFirst()
                            .orElse(record);
                }
            }
        }
        if (firstPardoned != null) {
            pardonNoticeListener.accept(new PardonNotice(firstPardoned, staffName, silent));
        }
        return pardoned;
    }

    public void setExpiryListener(@NotNull Consumer<PunishmentRecord> listener) {
        this.expiryListener = listener;
    }

    public void fireExpiry(@NotNull PunishmentRecord record) {
        expiryListener.accept(record);
    }

    @NotNull
    public PunishmentRecord punish(@NotNull PunishmentRequest request) {
        Instant now = clock.instant();
        long expiresAtMillis = request.expiresAt() == null ? 0L : request.expiresAt().toEpochMilli();
        PunishmentRecord record = new PunishmentRecord(ids.incrementAndGet(), request.targetUuid(), request.targetName(), request.staffUuid(), request.staffName(), request.type(), request.reason(), request.proof(), now.toEpochMilli(), expiresAtMillis, request.silent(), request.clearInventory(), false, request.server());
        records.add(record);
        PunishmentNotice notice = new PunishmentNotice(record, !request.silent());
        protocolManager.publish(CHANNEL, notice);
        persistence.punishIssue(record);
        enforcer.accept(notice);
        return record;
    }

    public boolean pardon(long id) {
        return pardon(id, SYSTEM_STAFF, "");
    }

    public boolean pardon(long id, @NotNull UUID staff, @NotNull String reason) {
        for (PunishmentRecord record : records) {
            if (record.id() == id && !record.pardoned()) {
                records.remove(record);
                PunishmentRecord pardoned = new PunishmentRecord(record.id(), record.targetUuid(), record.targetName(), record.staffUuid(), record.staffName(), record.type(), record.reason(), record.proof(), record.createdAtMillis(), record.expiresAtMillis(), record.silent(), record.clearInventory(), true, record.server());
                records.add(pardoned);
                persistence.punishPardon(id, staff, reason);
                pardonListener.accept(pardoned);
                return true;
            }
        }
        return false;
    }

    public int pardonActive(@NotNull UUID targetUuid,
                            @NotNull Set<PunishmentType> types,
                            @NotNull UUID staff,
                            @NotNull String reason) {
        int pardoned = 0;
        long nowMillis = clock.instant().toEpochMilli();
        for (PunishmentRecord record : List.copyOf(records)) {
            if (record.targetUuid().equals(targetUuid)
                    && types.contains(record.type())
                    && record.active(nowMillis)
                    && pardon(record.id(), staff, reason)) {
                pardoned++;
            }
        }
        return pardoned;
    }

    public int clearHistory(@NotNull UUID targetUuid) {
        return clearHistory(targetUuid, SYSTEM_STAFF);
    }

    public int clearHistory(@NotNull UUID targetUuid, @NotNull UUID staff) {
        int before = records.size();
        records.removeIf(record -> record.targetUuid().equals(targetUuid));
        int removed = before - records.size();
        if (removed > 0) {
            persistence.punishClearHistory(targetUuid, staff);
        }
        return removed;
    }

    public void applyRecord(@NotNull PunishmentRecord record) {
        records.removeIf(existing -> existing.id() == record.id());
        records.add(record);
        long observed = record.id();
        long current = ids.get();
        if (observed > current) {
            ids.compareAndSet(current, observed);
        }
    }

    public void removeRecord(long recordId) {
        records.removeIf(r -> r.id() == recordId);
    }

    public void applyTemplateRow(@NotNull PunishmentTemplate template) {
        templates.removeIf(t -> normalize(t.title()).equals(normalize(template.title())));
        templates.add(template);
    }

    public void removeTemplateRow(@NotNull String title) {
        templates.removeIf(t -> normalize(t.title()).equals(normalize(title)));
    }

    @NotNull
    public List<PunishmentRecord> history(@NotNull UUID targetUuid) {
        return records.stream()
                .filter(record -> record.targetUuid().equals(targetUuid))
                .sorted(Comparator.comparingLong(PunishmentRecord::createdAtMillis).reversed())
                .toList();
    }

    @NotNull
    public List<PunishmentRecord> all() {
        return records.stream()
                .sorted(Comparator.comparingLong(PunishmentRecord::createdAtMillis).reversed())
                .toList();
    }

    @NotNull
    public List<PunishmentRecord> byType(@NotNull PunishmentType type) {
        return records.stream()
                .filter(record -> record.type() == type)
                .sorted(Comparator.comparingLong(PunishmentRecord::createdAtMillis).reversed())
                .toList();
    }

    public int clearByType(@NotNull PunishmentType type, @NotNull UUID staff) {
        int before = records.size();
        records.removeIf(record -> record.type() == type);
        int removed = before - records.size();
        if (removed > 0) {
            persistence.punishClearHistory(SYSTEM_STAFF, staff);
        }
        return removed;
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

    @NotNull
    public PunishmentTemplate addTemplate(@NotNull PunishmentCategory category, @NotNull String duration, @NotNull String title, @NotNull String information) {
        return addTemplateInternal(category, duration, title, information, false);
    }

    @NotNull
    public PunishmentTemplate seedTemplate(@NotNull PunishmentCategory category, @NotNull String duration, @NotNull String title, @NotNull String information) {
        return addTemplateInternal(category, duration, title, information, true);
    }

    @NotNull
    private PunishmentTemplate addTemplateInternal(@NotNull PunishmentCategory category, @NotNull String duration, @NotNull String title, @NotNull String information, boolean seeded) {
        removeTemplate(title);
        PunishmentTemplate template = new PunishmentTemplate(category, duration, title, information);
        templates.add(template);
        persistence.templateUpsert(template, seeded);
        return template;
    }

    public boolean editTemplate(@NotNull String title, @NotNull PunishmentCategory category, @NotNull String duration, @NotNull String nextTitle, @NotNull String information) {
        PunishmentTemplate existing = template(title);
        if (existing == null) {
            return false;
        }
        templates.remove(existing);

        if (!normalize(title).equals(normalize(nextTitle))) {
            persistence.templateRemove(title);
        }
        PunishmentTemplate updated = new PunishmentTemplate(category, duration, nextTitle, information);
        templates.add(updated);
        persistence.templateUpsert(updated, false);
        return true;
    }

    public boolean removeTemplate(@NotNull String title) {
        boolean removed = templates.removeIf(template -> normalize(template.title()).equals(normalize(title)));
        if (removed) {
            persistence.templateRemove(title);
        }
        return removed;
    }

    public PunishmentTemplate template(@NotNull String title) {
        return templates.stream()
                .filter(template -> normalize(template.title()).equals(normalize(title)))
                .findFirst()
                .orElse(null);
    }

    @NotNull
    public List<PunishmentTemplate> templates() {
        return templates.stream()
                .sorted(Comparator.comparing(PunishmentTemplate::category).thenComparing(PunishmentTemplate::title))
                .toList();
    }

    @NotNull
    public List<PunishmentTemplate> templates(@NotNull PunishmentCategory category) {
        return templates().stream()
                .filter(template -> template.category() == category)
                .toList();
    }

    private void receive(@NotNull PunishmentNotice notice) {
        notices.add(notice);
    }

    @NotNull
    private static String normalize(@NotNull String input) {
        return input.trim().toLowerCase(Locale.ROOT);
    }
}
