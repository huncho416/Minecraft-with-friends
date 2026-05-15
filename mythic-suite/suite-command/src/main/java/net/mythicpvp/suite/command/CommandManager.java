package net.mythicpvp.suite.command;

import net.mythicpvp.suite.hex.MythicHex;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CommandManager {

    private final JavaPlugin plugin;
    private final Map<String, RegisteredCommand> commands = new ConcurrentHashMap<>();
    private final Map<Class<?>, Function<String, ?>> resolvers = new ConcurrentHashMap<>();
    private final Map<String, CommandCompletionProvider> completions = new ConcurrentHashMap<>();
    private final CommandBlocker commandBlocker;

    public CommandManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        registerDefaultResolvers();
        this.commandBlocker = new CommandBlocker(plugin, this);
        plugin.getServer().getPluginManager().registerEvents(commandBlocker, plugin);
    }

    private void registerDefaultResolvers() {
        resolvers.put(String.class, s -> s);
        resolvers.put(int.class, Integer::parseInt);
        resolvers.put(Integer.class, Integer::parseInt);
        resolvers.put(long.class, Long::parseLong);
        resolvers.put(Long.class, Long::parseLong);
        resolvers.put(double.class, Double::parseDouble);
        resolvers.put(Double.class, Double::parseDouble);
        resolvers.put(boolean.class, Boolean::parseBoolean);
        resolvers.put(Boolean.class, Boolean::parseBoolean);
        resolvers.put(Player.class, s -> plugin.getServer().getPlayer(s));
    }

    public <T> void registerResolver(@NotNull Class<T> type, @NotNull Function<String, T> resolver) {
        resolvers.put(type, resolver);
    }

    public void registerCompletion(@NotNull String id, @NotNull CommandCompletionProvider provider) {
        completions.put(id.toLowerCase(), provider);
    }

    @NotNull
    public CommandBlocker getCommandBlocker() {
        return commandBlocker;
    }

    public void register(@NotNull MythicCommand command) {
        CommandAlias aliasAnnotation = command.getClass().getAnnotation(CommandAlias.class);
        if (aliasAnnotation == null) {
            throw new IllegalArgumentException("Command must have @CommandAlias annotation");
        }

        String[] aliases = aliasAnnotation.value().split("\\|");
        String primaryAlias = aliases[0].toLowerCase();
        CommandPermission permAnnotation = command.getClass().getAnnotation(CommandPermission.class);

        RegisteredCommand registered = new RegisteredCommand(command, permAnnotation != null ? permAnnotation.value() : null);

        for (Method method : command.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(Default.class)) {
                registered.setDefaultHandler(method);
            } else if (method.isAnnotationPresent(Subcommand.class)) {
                String sub = method.getAnnotation(Subcommand.class).value().toLowerCase();
                registered.addSubcommand(sub, method);
            }
        }

        for (String alias : aliases) {
            commands.put(alias.toLowerCase(), registered);
        }

        Command bukkitCommand = new Command(primaryAlias, "", "", Arrays.asList(aliases)) {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                CommandManager.this.handleCommand(sender, primaryAlias, args);
                return true;
            }

            @Override
            @NotNull
            public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                return CommandManager.this.tabComplete(sender, primaryAlias, args);
            }
        };

        if (registered.getPermission() != null) {
            bukkitCommand.setPermission(registered.getPermission());
        }
        plugin.getServer().getCommandMap().register(plugin.getName().toLowerCase(), bukkitCommand);
    }

    private void handleCommand(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        RegisteredCommand registered = commands.get(alias);
        if (registered == null) return;

        if (registered.getPermission() != null && !sender.hasPermission(registered.getPermission())) {
            commandBlocker.sendDenied(sender);
            return;
        }

        Method handler;
        String[] handlerArgs;

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            handler = registered.getSubcommand(sub);
            if (handler != null) {
                CommandPermission subPerm = handler.getAnnotation(CommandPermission.class);
                if (subPerm != null && !sender.hasPermission(subPerm.value())) {
                    commandBlocker.sendDenied(sender);
                    return;
                }
                handlerArgs = Arrays.copyOfRange(args, 1, args.length);
            } else {
                handler = registered.getDefaultHandler();
                handlerArgs = args;
            }
        } else {
            handler = registered.getDefaultHandler();
            handlerArgs = args;
        }

        if (handler == null) {
            sender.sendMessage(MythicHex.colorize("&#FF00F8✘ &#FFFFFFUnknown command usage."));
            return;
        }

        invokeHandler(sender, registered.getCommand(), handler, handlerArgs);
    }

    private void invokeHandler(@NotNull CommandSender sender, @NotNull MythicCommand command, @NotNull Method method, @NotNull String[] args) {
        Parameter[] params = method.getParameters();
        Object[] resolved = new Object[params.length];
        int argIndex = 0;

        for (int i = 0; i < params.length; i++) {
            Class<?> type = params[i].getType();

            if (CommandSender.class.isAssignableFrom(type)) {
                resolved[i] = sender;
                continue;
            }
            if (Player.class.isAssignableFrom(type) && sender instanceof Player) {
                resolved[i] = sender;
                continue;
            }
            if (Player.class.isAssignableFrom(type) && !(sender instanceof Player)) {
                sender.sendMessage(MythicHex.colorize("&#FF00F8✘ &#FFFFFFThis command is player-only."));
                return;
            }
            if (type.equals(String[].class)) {
                resolved[i] = Arrays.copyOfRange(args, argIndex, args.length);
                argIndex = args.length;
                continue;
            }

            if (argIndex >= args.length) {
                if (params[i].isAnnotationPresent(net.mythicpvp.suite.command.Optional.class)) {
                    resolved[i] = null;
                    continue;
                }
                sender.sendMessage(MythicHex.colorize("&#FF00F8✘ &#FFFFFFMissing argument: " + params[i].getName()));
                return;
            }

            Function<String, ?> resolver = resolvers.get(type);
            if (resolver != null) {
                try {
                    resolved[i] = resolver.apply(args[argIndex++]);
                } catch (Exception e) {
                    sender.sendMessage(MythicHex.colorize("&#FF00F8✘ &#FFFFFFInvalid argument: " + args[argIndex - 1]));
                    return;
                }
            } else {
                resolved[i] = args[argIndex++];
            }
        }

        try {
            method.invoke(command, resolved);
        } catch (Exception e) {
            sender.sendMessage(MythicHex.colorize("&#FF00F8✘ &#FFFFFFAn error occurred."));
            e.printStackTrace();
        }
    }

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        RegisteredCommand registered = commands.get(alias);
        if (registered == null) return Collections.emptyList();
        if (!commandBlocker.canSee(sender, alias)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subcommands = registered.getSubcommandNames().stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .filter(s -> {
                        String permission = getSubcommandPermission(alias, s);
                        return permission == null || sender.hasPermission(permission);
                    })
                    .collect(Collectors.toList());
            if (!subcommands.isEmpty()) {
                return subcommands;
            }
        }

        Method handler = null;
        String subcommand = "";
        String[] completionArgs = args;
        if (args.length > 0) {
            Method subHandler = registered.getSubcommand(args[0].toLowerCase());
            if (subHandler != null) {
                CommandPermission subPerm = subHandler.getAnnotation(CommandPermission.class);
                if (subPerm != null && !sender.hasPermission(subPerm.value())) {
                    return Collections.emptyList();
                }
                handler = subHandler;
                subcommand = args[0].toLowerCase();
                completionArgs = Arrays.copyOfRange(args, 1, args.length);
            }
        }
        if (handler == null) {
            handler = registered.getDefaultHandler();
        }
        if (handler == null) {
            return Collections.emptyList();
        }
        Complete completion = handler.getAnnotation(Complete.class);
        if (completion == null) {
            return Collections.emptyList();
        }
        int index = Math.max(0, completionArgs.length - 1);
        if (index >= completion.value().length) {
            index = completion.value().length - 1;
        }
        String id = completion.value()[index].toLowerCase();
        CommandCompletionProvider provider = completions.get(id);
        if (provider == null) {
            return Collections.emptyList();
        }
        CommandCompletionContext context = new CommandCompletionContext(sender, alias, subcommand, completionArgs);
        String current = context.current().toLowerCase();
        return provider.complete(context).stream()
                .filter(value -> value.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }

    String getPermission(@NotNull String command) {
        RegisteredCommand registered = commands.get(CommandBlocker.normalizeCommand(command));
        return registered == null ? null : registered.getPermission();
    }

    String getSubcommandPermission(@NotNull String command, @NotNull String subcommand) {
        RegisteredCommand registered = commands.get(CommandBlocker.normalizeCommand(command));
        if (registered == null) return null;
        Method method = registered.getSubcommand(subcommand.toLowerCase());
        if (method == null) return null;
        CommandPermission permission = method.getAnnotation(CommandPermission.class);
        return permission == null ? null : permission.value();
    }

    private static class RegisteredCommand {
        private final MythicCommand command;
        private final String permission;
        private Method defaultHandler;
        private final Map<String, Method> subcommands = new HashMap<>();

        RegisteredCommand(@NotNull MythicCommand command, String permission) {
            this.command = command;
            this.permission = permission;
        }

        MythicCommand getCommand() { return command; }
        String getPermission() { return permission; }
        Method getDefaultHandler() { return defaultHandler; }
        void setDefaultHandler(Method m) { this.defaultHandler = m; }
        void addSubcommand(String name, Method m) { subcommands.put(name, m); }
        Method getSubcommand(String name) { return subcommands.get(name); }
        Set<String> getSubcommandNames() { return subcommands.keySet(); }
    }
}
