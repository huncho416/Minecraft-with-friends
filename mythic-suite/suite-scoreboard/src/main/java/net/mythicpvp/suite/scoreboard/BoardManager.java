package net.mythicpvp.suite.scoreboard;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BoardManager {

    private static final BoardManager INSTANCE = new BoardManager();
    private final Map<UUID, MythicBoard> boards = new ConcurrentHashMap<>();

    private BoardManager() {}

    @NotNull
    public static BoardManager getInstance() {
        return INSTANCE;
    }

    @NotNull
    public MythicBoard create(@NotNull Player player, @NotNull String title) {
        remove(player);
        MythicBoard board = new MythicBoard(player, title);
        boards.put(player.getUniqueId(), board);
        return board;
    }

    @Nullable
    public MythicBoard get(@NotNull Player player) {
        return boards.get(player.getUniqueId());
    }

    public void remove(@NotNull Player player) {
        MythicBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.remove();
        }
    }

    public void removeAll() {
        boards.values().forEach(MythicBoard::remove);
        boards.clear();
    }
}
