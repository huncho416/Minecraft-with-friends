package net.mythicpvp.core.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class IpTracker implements Listener {

    private final Logger logger;
    private final Path file;
    private final Gson gson = new Gson();
    private final Map<UUID, Entry> byUuid = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, Long>> byIp = new ConcurrentHashMap<>();

    public IpTracker(@NotNull Logger logger, @NotNull File dataFolder) {
        this.logger = logger;
        this.file = new File(dataFolder, "ip-tracker.json").toPath();
        load();
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress() == null ? null : player.getAddress().getAddress().getHostAddress();
        if (ip == null || ip.isBlank()) return;
        record(player.getUniqueId(), player.getName(), ip);
        save();
    }

    public void record(@NotNull UUID uuid, @NotNull String name, @NotNull String ip) {
        Entry entry = byUuid.computeIfAbsent(uuid, k -> new Entry(k, name, new LinkedHashMap<>()));
        entry.name = name;
        entry.ips.put(ip, System.currentTimeMillis());
        byIp.computeIfAbsent(ip, k -> new ConcurrentHashMap<>()).put(uuid, System.currentTimeMillis());
    }

    @NotNull
    public List<Entry> altsOf(@NotNull UUID uuid) {
        Entry self = byUuid.get(uuid);
        if (self == null) return List.of();
        Map<UUID, Long> seen = new LinkedHashMap<>();
        for (String ip : self.ips.keySet()) {
            Map<UUID, Long> sharers = byIp.get(ip);
            if (sharers == null) continue;
            for (Map.Entry<UUID, Long> e : sharers.entrySet()) {
                if (e.getKey().equals(uuid)) continue;
                seen.merge(e.getKey(), e.getValue(), Math::max);
            }
        }
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<UUID, Long> e : seen.entrySet()) {
            Entry alt = byUuid.get(e.getKey());
            if (alt != null) out.add(alt);
        }
        return out;
    }

    @NotNull
    public List<DupeReport> duplicates() {
        List<DupeReport> reports = new ArrayList<>();
        for (Map.Entry<String, Map<UUID, Long>> e : byIp.entrySet()) {
            if (e.getValue().size() < 2) continue;
            List<Entry> entries = new ArrayList<>();
            for (UUID uuid : e.getValue().keySet()) {
                Entry entry = byUuid.get(uuid);
                if (entry != null) entries.add(entry);
            }
            if (entries.size() < 2) continue;
            reports.add(new DupeReport(e.getKey(), entries));
        }
        reports.sort((a, b) -> Integer.compare(b.players.size(), a.players.size()));
        return reports;
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            String body = Files.readString(file);
            if (body.isBlank()) return;
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("players")) return;
            for (Map.Entry<String, com.google.gson.JsonElement> e : root.getAsJsonObject("players").entrySet()) {
                UUID uuid;
                try { uuid = UUID.fromString(e.getKey()); } catch (IllegalArgumentException ex) { continue; }
                JsonObject row = e.getValue().getAsJsonObject();
                String name = row.has("name") ? row.get("name").getAsString() : uuid.toString();
                Map<String, Long> ips = new LinkedHashMap<>();
                if (row.has("ips")) {
                    for (Map.Entry<String, com.google.gson.JsonElement> ipEntry : row.getAsJsonObject("ips").entrySet()) {
                        ips.put(ipEntry.getKey(), ipEntry.getValue().getAsLong());
                    }
                }
                Entry entry = new Entry(uuid, name, ips);
                byUuid.put(uuid, entry);
                for (Map.Entry<String, Long> ipEntry : ips.entrySet()) {
                    byIp.computeIfAbsent(ipEntry.getKey(), k -> new ConcurrentHashMap<>())
                            .put(uuid, ipEntry.getValue());
                }
            }
        } catch (IOException | RuntimeException e) {
            logger.warning("[ip-tracker] failed to load: " + e.getMessage());
        }
    }

    private synchronized void save() {
        try {
            Map<String, Object> root = new HashMap<>();
            Map<String, Object> players = new LinkedHashMap<>();
            for (Entry entry : byUuid.values()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", entry.name);
                row.put("ips", entry.ips);
                players.put(entry.uuid.toString(), row);
            }
            root.put("players", players);
            Files.writeString(file, gson.toJson(root));
        } catch (IOException e) {
            logger.warning("[ip-tracker] failed to save: " + e.getMessage());
        }
    }

    public static final class Entry {
        public final UUID uuid;
        public String name;
        public final Map<String, Long> ips;

        Entry(@NotNull UUID uuid, @NotNull String name, @NotNull Map<String, Long> ips) {
            this.uuid = uuid;
            this.name = name;
            this.ips = ips;
        }
    }

    public record DupeReport(@NotNull String ip, @NotNull List<Entry> players) {}
}
