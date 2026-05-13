package net.mythicpvp.suite.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface MythicPlayer {

    @NotNull UUID getUniqueId();

    @NotNull String getName();

    @NotNull Optional<Player> getBukkitPlayer();

    @NotNull String getRank();

    void setRank(@NotNull String rank);

    long getCoins();

    void setCoins(long coins);

    long getPoints();

    void setPoints(long points);

    long getGems();

    void setGems(long gems);

    boolean hasPermission(@NotNull String permission);

    boolean isOnline();

    @NotNull String getCurrentServer();
}
