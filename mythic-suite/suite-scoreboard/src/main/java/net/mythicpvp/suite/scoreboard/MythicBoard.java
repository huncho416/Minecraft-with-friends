package net.mythicpvp.suite.scoreboard;

import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MythicBoard {

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final List<String> lines = new ArrayList<>();
    private String title;

    public MythicBoard(@NotNull Player player, @NotNull String title) {
        this.player = player;
        this.title = title;
        this.scoreboard = player.getServer().getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("mythic", Criteria.DUMMY, MythicHex.colorize(title));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
    }

    @NotNull
    public MythicBoard setTitle(@NotNull String title) {
        this.title = title;
        objective.displayName(MythicHex.colorize(title));
        return this;
    }

    @NotNull
    public MythicBoard setLines(@NotNull List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        rebuild();
        return this;
    }

    @NotNull
    public MythicBoard setLine(int index, @NotNull String text) {
        while (lines.size() <= index) {
            lines.add("");
        }
        lines.set(index, text);
        rebuild();
        return this;
    }

    private void rebuild() {
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String entry = MythicHex.toLegacy(MythicHex.colorize(line));

            if (entry.isBlank()) {
                entry = " ".repeat(i + 1);
            }

            objective.getScore(entry).setScore(lines.size() - i);
        }
    }

    public void remove() {
        player.setScoreboard(player.getServer().getScoreboardManager().getMainScoreboard());
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }
}
