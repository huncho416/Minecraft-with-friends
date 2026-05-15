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

        Class<?> scan = command.getClass();
        while (scan != null && scan != Object.class) {
            for (Method method : scan.getDeclaredMethods()) {
                method.setAccessible(true);
                if (method.isAnnotationPresent(Default.class) && registered.getDefaultHandler() == null) {
                    registered.setDefaultHandler(method);
                } else if (method.isAnnotationPresent(Subcommand.class)) {
                    String sub = method.getAnnotation(Subcommand.class).value().toLowerCase();
                    if (registered.getSubcommand(sub) == null) {
                        registered.addSubcommand(sub, method);
                    }
                }
            }
            scan = scan.getSuperclass();
        }

        for (String alias : aliases) {
            commands.put(alias.toLowerCase(), registered);
        }

        Command bukkitCommand = new Command(primaryAlias, "", "", Arrays.asList(aliases)) {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                CommandManager.this.handleCommand(sender, label.toLowerCase(Locale.ROOT), args);
                return true;
            }

            @Override
            @NotNull
            public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                return CommandManager.this.tabComplete(sender, alias.toLowerCase(Locale.ROOT), args);
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
                sender.sendMessage(MythicHex.colorize(usageHint(method, command)));
                return;
            }

            Function<String, ?> resolver = resolvers.get(type);
            if (resolver != null) {
                try {
                    resolved[i] = resolver.apply(args[argIndex++]);
                } catch (Exception e) {
                    sender.sendMessage(MythicHex.colorize("&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» &#FF8A8AInvalid argument: &#FFFFFF" + args[argIndex - 1]));
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
    private String usageHint(@NotNull Method method, @NotNull MythicCommand command) {
        Usage methodUsage = method.getAnnotation(Usage.class);
        if (methodUsage != null && !methodUsage.value().isBlank()) {
            return prefixed(methodUsage.value());
        }
        Usage classUsage = command.getClass().getAnnotation(Usage.class);
        if (classUsage != null && !classUsage.value().isBlank()) {
            return prefixed(classUsage.value());
        }
        return prefixed(autoUsage(method, command));
    }

    @NotNull
    private static String autoUsage(@NotNull Method method, @NotNull MythicCommand command) {
        StringBuilder sb = new StringBuilder("&#FF8A8AUsage: &#FFFFFF/").append(commandLabel(command));
        Subcommand sub = method.getAnnotation(Subcommand.class);
        if (sub != null) {
            sb.append(' ').append(sub.value());
        }
        for (Parameter param : method.getParameters()) {
            Class<?> type = param.getType();
            if (CommandSender.class.isAssignableFrom(type)) {
                continue;
            }
            if (Player.class.isAssignableFrom(type) && param.isAnnotationPresent(Default.class)) {
                continue;
            }
            if (CommandSender.class.isAssignableFrom(type) || type.equals(org.bukkit.entity.Player.class)) {
                continue;
            }
            boolean optional = param.isAnnotationPresent(net.mythicpvp.suite.command.Optional.class);
            String name = paramLabel(param);
            sb.append(' ').append(optional ? "[" : "<").append(name).append(optional ? "]" : ">");
        }
        return sb.toString();
    }

    @NotNull
    private static String paramLabel(@NotNull Parameter param) {
        if (param.getType().equals(String[].class)) {
            return "args...";
        }
        String raw = param.getName();
        if (raw == null || raw.startsWith("arg")) {
            String type = param.getType().getSimpleName().toLowerCase(Locale.ROOT);
            return type;
        }
        return raw;
    }

    @NotNull
    private static String commandLabel(@NotNull MythicCommand command) {
        CommandAlias alias = command.getClass().getAnnotation(CommandAlias.class);
        if (alias == null) {
            return command.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        }
        String[] aliases = alias.value().split("\\|");
        return aliases[0].toLowerCase(Locale.ROOT);
    }

    @NotNull
    private static String prefixed(@NotNull String body) {
        return "&#F529BE&lM&#FD37F0&ly&#F639EA&lt&#DD35C4&lh&#F63DF1&li&#EA21FF&lc&#FFFFFF&lP&#D2D8E0&lv&#DDDBD9&lP  &8» " + body;
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
