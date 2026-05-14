package net.mythicpvp.core;

import net.mythicpvp.core.chat.ChatControlService;
import net.mythicpvp.core.chat.ChatGuard;
import net.mythicpvp.core.command.CGrantCommand;
import net.mythicpvp.core.command.ChatCommand;
import net.mythicpvp.core.command.ClearGrantsCommand;
import net.mythicpvp.core.command.CoreCompletions;
import net.mythicpvp.core.command.DiscordCommand;
import net.mythicpvp.core.command.GamemodeCommand;
import net.mythicpvp.core.command.GmcCommand;
import net.mythicpvp.core.command.GmsCommand;
import net.mythicpvp.core.command.GrantCommand;
import net.mythicpvp.core.command.GrantsCommand;
import net.mythicpvp.core.command.HelpCommand;
import net.mythicpvp.core.command.HistoryCommand;
import net.mythicpvp.core.command.PunishCommand;
import net.mythicpvp.core.command.PunishmentAddCommand;
import net.mythicpvp.core.command.PunishmentEditCommand;
import net.mythicpvp.core.command.PunishmentRemoveCommand;
import net.mythicpvp.core.command.PunishmentsCommand;
import net.mythicpvp.core.command.RankEditorCommand;
import net.mythicpvp.core.command.ClearPunishmentsCommand;
import net.mythicpvp.core.command.TeleportCommand;
import net.mythicpvp.core.command.TpHereCommand;
import net.mythicpvp.core.config.CoreMessages;
import net.mythicpvp.core.display.DisplayService;
import net.mythicpvp.core.display.PlayerSessionListener;
import net.mythicpvp.core.essentials.CoreEssentialsService;
import net.mythicpvp.core.persistence.CoreHydrationSink;
import net.mythicpvp.core.persistence.MainThreadHydrationSink;
import net.mythicpvp.core.persistence.NoopPersistenceGateway;
import net.mythicpvp.core.persistence.PersistenceGateway;
import net.mythicpvp.core.persistence.StdbPersistenceGateway;
import net.mythicpvp.core.punishment.PunishmentCategory;
import net.mythicpvp.core.punishment.PunishmentLoginGuard;
import net.mythicpvp.core.punishment.PunishmentMenuService;
import net.mythicpvp.core.punishment.PunishmentService;
import net.mythicpvp.core.prompt.ChatPromptService;
import net.mythicpvp.core.rank.GrantFlowService;
import net.mythicpvp.core.rank.GrantService;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.core.staff.StaffChannelService;
import net.mythicpvp.core.staff.StaffPresenceService;
import net.mythicpvp.suite.api.MythicPlugin;
import net.mythicpvp.suite.command.CommandManager;
import net.mythicpvp.suite.config.ConfigManager;
import net.mythicpvp.suite.config.ConfigText;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.schema.MythicSchema;
import net.mythicpvp.suite.menu.MenuListener;
import net.mythicpvp.suite.protocol.ProtocolManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;

public class MythicCorePlugin extends JavaPlugin implements MythicPlugin {

    private ServerIdentity serverIdentity;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private StaffChannelService staffChannelService;
    private StaffPresenceService staffPresenceService;
    private PunishmentService punishmentService;
    private PunishmentMenuService punishmentMenuService;
    private ChatControlService chatControlService;
    private RankService rankService;
    private GrantService grantService;
    private GrantFlowService grantFlowService;
    private ChatPromptService chatPromptService;
    private CoreMessages messages;
    private CoreEssentialsService essentialsService;
    private PersistenceGateway persistenceGateway;
    private DisplayService displayService;
    private CoreHydrationSink hydrationSink;

    @Override
    public void onEnable() {
        enableTracked();
    }

    @Override
    public void onDisable() {
        disableTracked();
    }

    @Override
    public void enable() {
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("core.yml");
        saveResourceIfMissing("staff-channels.yml");
        saveResourceIfMissing("punishments.yml");
        saveResourceIfMissing("scoreboard.yml");
        saveResourceIfMissing("tablist.yml");
        saveResourceIfMissing("nametag.yml");
        saveResourceIfMissing("ranks.yml");
        saveResourceIfMissing("command-blocker.yml");
        serverIdentity = ServerIdentity.fromEnvironment();
        configManager = new ConfigManager(this);
        messages = new CoreMessages(new ConfigText(configManager.getOrCreate("messages"), "messages"));
        essentialsService = new CoreEssentialsService(messages);
        commandManager = new CommandManager(this);
        // Bring up the persistence gateway BEFORE rank/grant/punishment
        // services so YAML seeding writes through to STDB on first boot.
        persistenceGateway = createPersistenceGateway();
        rankService = new RankService();
        rankService.setPersistence(persistenceGateway);
        rankService.load(configManager.getOrCreate("ranks"));
        chatPromptService = new ChatPromptService(this);
        grantService = new GrantService(rankService, Clock.systemUTC());
        grantService.setPersistence(persistenceGateway);
        grantFlowService = new GrantFlowService(rankService, grantService, chatPromptService);
        ProtocolManager protocolManager = ProtocolManager.getInstance();
        punishmentService = new PunishmentService(protocolManager, Clock.systemUTC());
        punishmentService.setPersistence(persistenceGateway);
        punishmentMenuService = new PunishmentMenuService(punishmentService, chatPromptService, Clock.systemUTC(), serverIdentity.id());
        seedPunishments(configManager.getOrCreate("punishments"));
        // Open subscriptions to every Phase 3 table. Snapshot rows arrive
        // asynchronously and fold into the in-memory services via the
        // CoreHydrationSink. Wrapped in MainThreadHydrationSink so the
        // PermissionManager + Bukkit-side state stays main-thread-safe.
        // No-op when the gateway is the noop fallback (single-server).
        hydrationSink = new CoreHydrationSink(getLogger(), rankService, grantService, punishmentService);
        persistenceGateway.hydrate(new MainThreadHydrationSink(this, hydrationSink));
        // Login enforcement uses the hydrated blacklist + active
        // punishments. Registered ASAP so the first connecting player
        // sees the right answer (subscriptions deliver snapshot rows
        // synchronously via service_completed_successfully ordering).
        getServer().getPluginManager().registerEvents(
                new PunishmentLoginGuard(punishmentService, hydrationSink, messages), this);
        // Display tier — pushes tab/scoreboard/nametag through the suite
        // managers, sourcing rank state from RankService/GrantService.
        // Wired AFTER rank/grant services exist; refresh callbacks let
        // those services notify display on every mutation.
        displayService = new DisplayService(this, rankService, grantService, serverIdentity.id());
        displayService.loadTemplates(
                configManager.getOrCreate("tablist"),
                configManager.getOrCreate("scoreboard"));
        rankService.setDisplayRefresher(displayService::applyAll);
        grantService.setDisplayRefresher(displayService::refresh);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(displayService), this);
        CoreCompletions.register(commandManager, rankService, punishmentService);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(chatPromptService, this);
        commandManager.register(new GrantCommand(grantFlowService));
        commandManager.register(new GrantsCommand(grantService, rankService));
        commandManager.register(new CGrantCommand(grantService));
        commandManager.register(new ClearGrantsCommand(grantService));
        commandManager.register(new RankEditorCommand(rankService));
        commandManager.register(new PunishCommand(punishmentMenuService));
        commandManager.register(new PunishmentsCommand(punishmentMenuService));
        commandManager.register(new HistoryCommand(punishmentService, punishmentMenuService));
        commandManager.register(new ClearPunishmentsCommand(punishmentService));
        commandManager.register(new PunishmentAddCommand(punishmentService));
        commandManager.register(new PunishmentRemoveCommand(punishmentService));
        commandManager.register(new PunishmentEditCommand(punishmentService));
        commandManager.register(new GamemodeCommand(essentialsService));
        commandManager.register(new GmcCommand(essentialsService));
        commandManager.register(new GmsCommand(essentialsService));
        commandManager.register(new TeleportCommand(essentialsService));
        commandManager.register(new TpHereCommand(essentialsService));
        commandManager.register(new HelpCommand(essentialsService));
        commandManager.register(new DiscordCommand(essentialsService));
        staffChannelService = new StaffChannelService(protocolManager, serverIdentity.id());
        staffPresenceService = new StaffPresenceService(protocolManager, serverIdentity.id());
        // Pass shardId so LOCAL-scope chat-control changes from other
        // servers are dropped — see ChatControlService.apply.
        chatControlService = new ChatControlService(protocolManager, serverIdentity.id());
        ChatGuard chatGuard = new ChatGuard(this, chatControlService, messages);
        getServer().getPluginManager().registerEvents(chatGuard, this);
        commandManager.register(new ChatCommand(chatControlService, messages));
    }

    @Override
    public void disable() {
        // Order matters here. Save persistent state first so a slow
        // shutdown doesn't lose unsaved YAML edits, then tear down
        // visible UI surfaces, then close network connections.
        if (configManager != null) {
            try {
                configManager.saveAll();
            } catch (RuntimeException e) {
                getLogger().warning("config save during disable failed: " + e.getMessage());
            }
        }
        // Drop per-player UI state so a re-enable starts from a clean slate.
        // Each manager's clear() is idempotent and safe even if it was
        // never populated (e.g. plugin disabled before any join).
        try {
            net.mythicpvp.suite.tab.TabManager.getInstance().clear();
            net.mythicpvp.suite.nametag.NametagManager.getInstance().clear();
            net.mythicpvp.suite.scoreboard.BoardManager.getInstance().removeAll();
        } catch (RuntimeException e) {
            getLogger().warning("UI manager teardown failed: " + e.getMessage());
        }
        // Tear down STDB so the plugin can be cleanly re-enabled (Folia
        // /reload, Pterodactyl restart). disconnectAll is a no-op when
        // no connection was ever established.
        DatabaseManager.getInstance().disconnectAll();
    }

    @Override
    public void reload() {
        if (configManager != null) {
            configManager.reloadAll();
        }
        if (commandManager != null) {
            commandManager.getCommandBlocker().reload();
        }
    }

    @Override
    @NotNull
    public String getServerIdentifier() {
        return serverIdentity == null ? "local" : serverIdentity.id();
    }

    @NotNull
    public StaffChannelService staffChannelService() {
        return staffChannelService;
    }

    @NotNull
    public StaffPresenceService staffPresenceService() {
        return staffPresenceService;
    }

    @NotNull
    public PunishmentService punishmentService() {
        return punishmentService;
    }

    @NotNull
    public PunishmentMenuService punishmentMenuService() {
        return punishmentMenuService;
    }

    @NotNull
    public ChatControlService chatControlService() {
        return chatControlService;
    }

    @NotNull
    public RankService rankService() {
        return rankService;
    }

    @NotNull
    public GrantService grantService() {
        return grantService;
    }

    @NotNull
    public GrantFlowService grantFlowService() {
        return grantFlowService;
    }

    @NotNull
    public ChatPromptService chatPromptService() {
        return chatPromptService;
    }

    @NotNull
    public CoreMessages messages() {
        return messages;
    }

    @NotNull
    public CoreEssentialsService essentialsService() {
        return essentialsService;
    }

    private void saveResourceIfMissing(@NotNull String path) {
        if (!getDataFolder().toPath().resolve(path).toFile().exists()) {
            saveResource(path, false);
        }
    }

    private void seedPunishments(@NotNull MythicConfig config) {
        if (!punishmentService.templates().isEmpty()) {
            return;
        }
        ConfigurationSection section = config.getConfig().getConfigurationSection("punishments.templates");
        if (section == null) {
            punishmentService.seedTemplate(PunishmentCategory.WARN, "permanent", "General Warning", "Used for minor rule reminders.");
            punishmentService.seedTemplate(PunishmentCategory.MUTE, "1d", "Chat Offense #1", "First chat offense.");
            punishmentService.seedTemplate(PunishmentCategory.BAN, "30d", "Cheating #1", "First cheating offense.");
            punishmentService.seedTemplate(PunishmentCategory.BLACKLIST, "permanent", "Network Removal", "Severe network-level punishment.");
            return;
        }
        for (String id : section.getKeys(false)) {
            String path = "punishments.templates." + id + ".";
            punishmentService.seedTemplate(
                    PunishmentCategory.parse(config.getString(path + "category", "WARN")),
                    config.getString(path + "duration", "permanent"),
                    config.getString(path + "title", id),
                    config.getString(path + "information", "")
            );
        }
    }

    /**
     * Best-effort STDB connection. Returns the no-op gateway when:
     * <ul>
     *   <li>{@code STDB_URI} env var is unset (single-server / dev runs)
     *   <li>The STDB connection fails to construct
     * </ul>
     * Logs the chosen mode so ops know whether mutations are persisting.
     */
    @NotNull
    private PersistenceGateway createPersistenceGateway() {
        String uri = System.getenv("STDB_URI");
        String module = System.getenv().getOrDefault("STDB_MODULE", "mythicpvp");
        if (uri == null || uri.isBlank()) {
            getLogger().info("STDB_URI not set — mythic-core running in single-server / no-op persistence mode");
            return NoopPersistenceGateway.INSTANCE;
        }
        try {
            SpacetimeConnection connection = DatabaseManager.getInstance()
                    .createConnection("mythic-core", uri, module);
            // Connect asynchronously; the gateway tolerates calls before
            // the socket is fully open (writes return failed futures).
            connection.connect();
            MythicSchema schema = new MythicSchema(connection);
            getLogger().info("STDB persistence active: uri=" + uri + " module=" + module);
            return new StdbPersistenceGateway(getLogger(), schema, connection);
        } catch (Exception failure) {
            getLogger().warning("Failed to construct STDB connection (" + failure.getMessage()
                    + "); falling back to no-op persistence");
            return NoopPersistenceGateway.INSTANCE;
        }
    }
}
