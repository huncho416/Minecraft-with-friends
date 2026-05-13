package net.mythicpvp.suite.disguise;

import net.mythicpvp.suite.packet.PacketAction;
import net.mythicpvp.suite.packet.PacketSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class DisguiseManager {

    private static final DisguiseManager INSTANCE = new DisguiseManager();
    private final Map<UUID, DisguiseData> disguises = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> staffView = new ConcurrentHashMap<>();
    private final List<String> randomNames = new ArrayList<>(List.of(
            "MythicHero", "PinkKnight", "VoidRunner", "CrystalMage", "SkyDuelist", "StarChampion"
    ));

    private DisguiseManager() {}

    @NotNull
    public static DisguiseManager getInstance() {
        return INSTANCE;
    }

    public void disguise(@NotNull UUID player, @NotNull DisguiseData data) {
        disguises.put(player, data);
        refreshObservers(player);
    }

    @NotNull
    public DisguiseData disguiseAs(@NotNull UUID player, @NotNull String displayName, @Nullable SkinProperties skin, @Nullable String rankOverride) {
        DisguiseData data = new DisguiseData(displayName, skin, rankOverride, false);
        disguise(player, data);
        return data;
    }

    @NotNull
    public DisguiseData disguiseRandom(@NotNull UUID player) {
        String name = randomNames.get(ThreadLocalRandom.current().nextInt(randomNames.size()));
        DisguiseData data = new DisguiseData(name, null, null, true);
        disguise(player, data);
        return data;
    }

    public void setRandomNames(@NotNull List<String> names) {
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Random disguise names cannot be empty");
        }
        randomNames.clear();
        randomNames.addAll(names);
    }

    @NotNull
    public List<String> getRandomNames() {
        return Collections.unmodifiableList(randomNames);
    }

    public void undisguise(@NotNull UUID player) {
        disguises.remove(player);
        refreshObservers(player);
    }

    public boolean isDisguised(@NotNull UUID player) {
        return disguises.containsKey(player);
    }

    @Nullable
    public DisguiseData getDisguise(@NotNull UUID player) {
        return disguises.get(player);
    }

    @NotNull
    public String getDisplayName(@NotNull UUID player, @NotNull String realName) {
        DisguiseData data = disguises.get(player);
        return data != null ? data.displayName() : realName;
    }

    @NotNull
    public String getVisibleName(@NotNull UUID viewer, @NotNull UUID target, @NotNull String realName) {
        if (canSeeThrough(viewer) && isDisguised(target)) {
            return getDisplayName(target, realName) + " (" + realName + ")";
        }
        return getDisplayName(target, realName);
    }

    @Nullable
    public String getRankOverride(@NotNull UUID player) {
        DisguiseData data = disguises.get(player);
        return data == null ? null : data.rankOverride();
    }

    @Nullable
    public SkinProperties getSkin(@NotNull UUID player) {
        DisguiseData data = disguises.get(player);
        return data == null ? null : data.skin();
    }

    public void setStaffView(@NotNull UUID player, boolean enabled) {
        staffView.put(player, enabled);
    }

    public boolean canSeeThrough(@NotNull UUID player) {
        return staffView.getOrDefault(player, false);
    }

    public void refreshObservers(@NotNull UUID target) {
        if (Bukkit.getServer() == null) {
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(target);
        String fallbackName = targetPlayer == null ? target.toString() : targetPlayer.getName();
        DisguiseData data = disguises.get(target);
        String displayName = data == null ? fallbackName : data.displayName();
        SkinProperties skin = data == null ? null : data.skin();
        PacketAction action = new PacketAction.EntityRefresh(
                "disguise:" + target,
                target,
                displayName,
                skin == null ? null : skin.value(),
                skin == null ? null : skin.signature()
        );
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            PacketSession.getInstance().send(viewer, action);
        }
    }

    public void clear() {
        disguises.clear();
        staffView.clear();
    }

    public record SkinProperties(@NotNull String value, @NotNull String signature) {}

    public record DisguiseData(
            @NotNull String displayName,
            @Nullable SkinProperties skin,
            @Nullable String rankOverride,
            boolean random
    ) {
        @NotNull
        public static DisguiseData withName(@NotNull String name) {
            return new DisguiseData(name, null, null, false);
        }

        @NotNull
        public static DisguiseData full(@NotNull String name, @NotNull String skinValue, @NotNull String skinSignature, @Nullable String rankOverride) {
            return new DisguiseData(name, new SkinProperties(skinValue, skinSignature), rankOverride, false);
        }

        @Nullable
        public String skinValue() {
            return skin == null ? null : skin.value();
        }

        @Nullable
        public String skinSignature() {
            return skin == null ? null : skin.signature();
        }
    }
}
