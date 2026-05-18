package net.mythicpvp.suite.packet;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitPacketRenderer implements PacketRenderer {

    private static final String OBJECTIVE_NAME = "mythic";
    private static final int OBJECTIVE_ADD = 0;
    private static final int OBJECTIVE_REMOVE = 1;
    private static final int MAX_LINES = 15;

    private final Map<UUID, ScoreboardState> scoreboards = new ConcurrentHashMap<>();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    @Override
    public void render(@NotNull Player viewer, @NotNull PacketAction action) {
        try {
            if (action instanceof PacketAction.ScoreboardState scoreboard) {
                renderScoreboard(viewer, scoreboard);
            } else if (action instanceof PacketAction.NametagState nametag) {
                renderNametag(viewer, nametag);
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            warnOnce(action.getClass().getSimpleName(), failure);
        }
    }

    private void renderScoreboard(@NotNull Player viewer, @NotNull PacketAction.ScoreboardState state)
            throws ReflectiveOperationException {
        ScoreboardState previous = scoreboards.get(viewer.getUniqueId());
        List<Component> lines = state.lines().stream().limit(MAX_LINES).toList();
        int contentHash = contentHash(state.title(), lines);

        if (previous != null && previous.contentHash() == contentHash) {
            return;
        }

        if (previous != null && previous.lineCount() == lines.size()) {
            for (int index = 0; index < lines.size(); index++) {
                String owner = lineOwner(index);
                send(viewer, setScorePacket(owner, lines.get(index), lines.size() - index));
            }
            scoreboards.put(viewer.getUniqueId(), new ScoreboardState(previous.owners(), contentHash, lines.size()));
            return;
        }

        Object objective = newObjective(state.title());
        if (previous != null) {
            for (String owner : previous.owners()) {
                send(viewer, resetScorePacket(owner));
            }
            send(viewer, setObjectivePacket(objective, OBJECTIVE_REMOVE));
        }

        send(viewer, setObjectivePacket(objective, OBJECTIVE_ADD));
        send(viewer, displayObjectivePacket(objective));

        List<String> owners = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String owner = lineOwner(index);
            owners.add(owner);
            send(viewer, setScorePacket(owner, lines.get(index), lines.size() - index));
        }
        scoreboards.put(viewer.getUniqueId(), new ScoreboardState(owners, contentHash, lines.size()));
    }

    private static int contentHash(@NotNull Component title, @NotNull List<Component> lines) {
        int result = PlainTextComponentSerializer.plainText().serialize(title).hashCode();
        for (Component line : lines) {
            result = result * 31 + PlainTextComponentSerializer.plainText().serialize(line).hashCode();
        }
        return result;
    }

    private void renderNametag(@NotNull Player viewer, @NotNull PacketAction.NametagState state)
            throws ReflectiveOperationException {
        Object team = newPlayerTeam(teamName(state.sortWeight(), state.target()), state.prefix(), state.suffix());
        Player target = Bukkit.getPlayer(state.target());
        addPlayerToTeam(team, target == null ? state.displayName() : target.getName());
        send(viewer, playerTeamPacket(team, true));
    }

    private Object newObjective(@NotNull Component title) throws ReflectiveOperationException {
        Class<?> scoreboardClass = Class.forName("net.minecraft.world.scores.Scoreboard");
        Class<?> objectiveClass = Class.forName("net.minecraft.world.scores.Objective");
        Class<?> criteriaClass = Class.forName("net.minecraft.world.scores.criteria.ObjectiveCriteria");
        Class<?> renderTypeClass = Class.forName("net.minecraft.world.scores.criteria.ObjectiveCriteria$RenderType");
        Object scoreboard = scoreboardClass.getConstructor().newInstance();
        Object criteria = criteriaClass.getField("DUMMY").get(null);
        Object renderType = enumValue(renderTypeClass, "INTEGER");
        Constructor<?> constructor = objectiveClass.getConstructor(
                scoreboardClass,
                String.class,
                criteriaClass,
                Class.forName("net.minecraft.network.chat.Component"),
                renderTypeClass,
                boolean.class,
                Class.forName("net.minecraft.network.chat.numbers.NumberFormat"));
        return constructor.newInstance(scoreboard, OBJECTIVE_NAME, criteria, vanilla(title), renderType, false, null);
    }

    private Object setObjectivePacket(@NotNull Object objective, int mode) throws ReflectiveOperationException {
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetObjectivePacket");
        return packetClass.getConstructor(objective.getClass(), int.class).newInstance(objective, mode);
    }

    private Object displayObjectivePacket(@NotNull Object objective) throws ReflectiveOperationException {
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket");
        Class<?> slotClass = Class.forName("net.minecraft.world.scores.DisplaySlot");
        return packetClass.getConstructor(slotClass, objective.getClass()).newInstance(enumValue(slotClass, "SIDEBAR"), objective);
    }

    private Object setScorePacket(@NotNull String owner, @NotNull Component display, int score)
            throws ReflectiveOperationException {
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetScorePacket");
        return packetClass
                .getConstructor(String.class, String.class, int.class, Optional.class, Optional.class)
                .newInstance(owner, OBJECTIVE_NAME, score, Optional.of(vanilla(display)), Optional.empty());
    }

    private Object resetScorePacket(@NotNull String owner) throws ReflectiveOperationException {
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundResetScorePacket");
        return packetClass.getConstructor(String.class, String.class).newInstance(owner, OBJECTIVE_NAME);
    }

    private Object newPlayerTeam(@NotNull String name, @NotNull Component prefix, @NotNull Component suffix)
            throws ReflectiveOperationException {
        Class<?> scoreboardClass = Class.forName("net.minecraft.world.scores.Scoreboard");
        Class<?> teamClass = Class.forName("net.minecraft.world.scores.PlayerTeam");
        Object scoreboard = scoreboardClass.getConstructor().newInstance();
        Object team = teamClass.getConstructor(scoreboardClass, String.class).newInstance(scoreboard, name);
        call(team, "setDisplayName", vanilla(Component.text(name)));
        call(team, "setPlayerPrefix", vanilla(prefix));
        call(team, "setPlayerSuffix", vanilla(suffix));
        return team;
    }

    @SuppressWarnings("unchecked")
    private void addPlayerToTeam(@NotNull Object team, @NotNull String playerName) throws ReflectiveOperationException {
        Method playersMethod = method(team.getClass(), "getPlayers", 0);
        Object players = playersMethod.invoke(team);
        if (players instanceof Collection<?> collection) {
            ((Collection<String>) collection).add(playerName);
        }
    }

    private Object playerTeamPacket(@NotNull Object team, boolean updatePlayers) throws ReflectiveOperationException {
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket");
        Method factory = packetClass.getMethod("createAddOrModifyPacket", team.getClass(), boolean.class);
        return factory.invoke(null, team, updatePlayers);
    }

    private Object vanilla(@NotNull Component component) throws ReflectiveOperationException {
        try {
            Class<?> bridge = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            Method method = bridge.getDeclaredMethod("asVanilla", Component.class);
            method.setAccessible(true);
            return method.invoke(null, component);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            String plain = PlainTextComponentSerializer.plainText().serialize(component);
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method literal = componentClass.getMethod("literal", String.class);
            return literal.invoke(null, plain);
        }
    }

    private void send(@NotNull Player viewer, @NotNull Object packet) throws ReflectiveOperationException {
        Object handle = viewer.getClass().getMethod("getHandle").invoke(viewer);
        Field connectionField = handle.getClass().getField("connection");
        Object connection = connectionField.get(handle);
        Method send = method(connection.getClass(), "send", 1);
        send.invoke(connection, packet);
    }

    private void call(@NotNull Object target, @NotNull String name, @NotNull Object argument)
            throws ReflectiveOperationException {
        Method method = null;
        for (Method candidate : target.getClass().getMethods()) {
            if (candidate.getName().equals(name)
                    && candidate.getParameterCount() == 1
                    && candidate.getParameterTypes()[0].isAssignableFrom(argument.getClass())) {
                method = candidate;
                break;
            }
        }
        if (method == null) {
            throw new NoSuchMethodException(target.getClass().getName() + "#" + name);
        }
        method.invoke(target, argument);
    }

    @NotNull
    private Method method(@NotNull Class<?> type, @NotNull String name, int parameterCount) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    private Object enumValue(@NotNull Class<?> enumClass, @NotNull String name) {
        return Enum.valueOf(enumClass.asSubclass(Enum.class), name);
    }

    @NotNull
    private String lineOwner(int index) {
        return "mythic_line_" + index;
    }

    @NotNull
    private String teamName(int sortWeight, @NotNull UUID uuid) {
        int clamped = Math.max(0, Math.min(sortWeight, 999));
        int inverted = 999 - clamped;
        return ("%03d_%s".formatted(inverted, uuid.toString().replace("-", ""))).substring(0, 16);
    }

    private void warnOnce(@NotNull String key, @NotNull Exception failure) {
        if (warned.add(key)) {
            Bukkit.getLogger().warning("[MythicSuite] packet renderer failed for " + key + ": " + failure.getMessage());
        }
    }

    private record ScoreboardState(@NotNull List<String> owners, int contentHash, int lineCount) {}
}
