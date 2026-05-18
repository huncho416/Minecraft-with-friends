package net.mythicpvp.suite.scoreboard;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.config.ConfigText;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.packet.PacketAction;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MythicBoard {

    private static final boolean FOLIA = detectFolia();

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final List<String> lines = new ArrayList<>();
    private final List<String> animatedTitles = new ArrayList<>();
    private String title;
    private String fontKey = "";
    private int titleFrame;

    public MythicBoard(@NotNull Player player, @NotNull String title) {
        this.player = player;
        this.title = title;
        if (FOLIA) {
            this.scoreboard = null;
            this.objective = null;
        } else {
            this.scoreboard = player.getServer().getScoreboardManager().getNewScoreboard();
            this.objective = scoreboard.registerNewObjective("mythic", Criteria.DUMMY, MythicHex.colorize(title));
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(scoreboard);
        }
        emit();
    }

    @NotNull
    public MythicBoard setFontKey(@NotNull String fontKey) {
        this.fontKey = fontKey;
        rebuild();
        return this;
    }

    @NotNull
    public MythicBoard setTitle(@NotNull String title) {
        this.title = title;
        if (objective != null) {
            objective.displayName(MythicHex.font(fontKey, title));
        }
        emit();
        return this;
    }

    @NotNull
    public MythicBoard setAnimatedTitles(@NotNull List<String> titles) {
        animatedTitles.clear();
        animatedTitles.addAll(titles);
        titleFrame = 0;
        if (!animatedTitles.isEmpty()) {
            setTitle(animatedTitles.getFirst());
        }
        return this;
    }

    public void tickTitleAnimation() {
        if (animatedTitles.isEmpty()) {
            return;
        }
        titleFrame = (titleFrame + 1) % animatedTitles.size();
        setTitle(animatedTitles.get(titleFrame));
    }

    @NotNull
    public MythicBoard load(@NotNull ConfigText text, @NotNull String key) {
        setFontKey(text.raw(key + ".font", fontKey));
        setAnimatedTitles(text.list(key + ".titles", List.of(title)));
        setLines(text.list(key + ".lines", lines));
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
        if (index < 0) {
            throw new IllegalArgumentException("Line index cannot be negative");
        }
        while (lines.size() <= index) {
            lines.add("");
        }
        lines.set(index, text);
        rebuild();
        return this;
    }

    @NotNull
    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void rebuild() {
        if (scoreboard != null && objective != null) {
            java.util.LinkedHashMap<String, Integer> desired = new java.util.LinkedHashMap<>();
            for (int i = 0; i < lines.size(); i++) {
                String entry = MythicHex.toLegacy(MythicHex.font(fontKey, lines.get(i)));
                if (entry.isBlank()) {
                    entry = " ".repeat(i + 1);
                }
                desired.put(entry, lines.size() - i);
            }
            for (String existing : new java.util.ArrayList<>(scoreboard.getEntries())) {
                if (!desired.containsKey(existing)) {
                    scoreboard.resetScores(existing);
                }
            }
            for (var e : desired.entrySet()) {
                org.bukkit.scoreboard.Score score = objective.getScore(e.getKey());
                if (!score.isScoreSet() || score.getScore() != e.getValue()) {
                    score.setScore(e.getValue());
                }
            }
        }
        emit();
    }

    private void emit() {
        List<Component> rendered = lines.stream().map(line -> MythicHex.font(fontKey, line)).toList();
        PacketAction.send(player, new PacketAction.ScoreboardState("scoreboard:" + player.getUniqueId(), MythicHex.font(fontKey, title), rendered, fontKey));
    }

    public void remove() {
        if (!FOLIA) {
            player.setScoreboard(player.getServer().getScoreboardManager().getMainScoreboard());
        }
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }
}
