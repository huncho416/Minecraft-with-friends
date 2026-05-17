package net.mythicpvp.core;

import net.mythicpvp.core.announce.BroadcastService;
import net.mythicpvp.core.audit.CoreAuditLog;
import net.mythicpvp.core.chat.ChatColorMenuService;
import net.mythicpvp.core.chat.ChatColorService;
import net.mythicpvp.core.chat.ChatControlService;
import net.mythicpvp.core.chat.ChatFormatListener;
import net.mythicpvp.core.cosmetic.RankBundleGrantHook;
import net.mythicpvp.core.cosmetic.RankCosmeticBundles;
import net.mythicpvp.core.chat.ChatGuard;
import net.mythicpvp.core.command.AppealCommand;
import net.mythicpvp.core.command.AppealsCommand;
import net.mythicpvp.core.command.BroadcastCommand;
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
import net.mythicpvp.core.command.FriendCommand;
import net.mythicpvp.core.command.MailCommand;
import net.mythicpvp.core.command.PartyCommand;
import net.mythicpvp.core.command.PrivateMessageCommand;
import net.mythicpvp.core.command.PunishCommand;
import net.mythicpvp.core.command.PunishmentAddCommand;
import net.mythicpvp.core.command.PunishmentDirectCommand;
import net.mythicpvp.core.command.PunishmentEditCommand;
import net.mythicpvp.core.command.PunishmentRemoveCommand;
import net.mythicpvp.core.command.PunishmentsCommand;
import net.mythicpvp.core.command.RankEditorCommand;
import net.mythicpvp.core.command.ClearPunishmentsCommand;
import net.mythicpvp.core.command.StaffChatCommand;
import net.mythicpvp.core.command.StaffModeCommand;
import net.mythicpvp.core.command.TeleportCommand;
import net.mythicpvp.core.command.TpHereCommand;
import net.mythicpvp.core.command.VanishCommand;
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
import net.mythicpvp.core.staff.BukkitStaffAudience;
import net.mythicpvp.core.staff.BukkitStaffPresenceAudience;
import net.mythicpvp.core.staff.StaffChannelService;
import net.mythicpvp.core.staff.StaffChatToggleListener;
import net.mythicpvp.core.staff.StaffPresenceListener;
import net.mythicpvp.core.staff.StaffPresenceService;
import net.mythicpvp.core.staffmode.StaffModeService;
import net.mythicpvp.core.staffmode.StaffModeToolListener;
import net.mythicpvp.core.social.FriendLoginListener;
import net.mythicpvp.core.social.MailLoginListener;
import net.mythicpvp.core.social.OfflineRewardService;
import net.mythicpvp.core.social.SocialService;
import net.mythicpvp.suite.api.MythicPlugin;
import net.mythicpvp.suite.command.CommandManager;
import net.mythicpvp.suite.config.ConfigManager;
import net.mythicpvp.suite.config.ConfigText;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.database.DatabaseManager;
import net.mythicpvp.suite.database.SpacetimeConnection;
import net.mythicpvp.suite.database.schema.MythicSchema;
import net.mythicpvp.suite.database.schema.SchemaVersion;
import net.mythicpvp.suite.database.schema.ServerRole;
import net.mythicpvp.suite.database.schema.ServerStatus;
import net.mythicpvp.suite.menu.MenuListener;
import net.mythicpvp.suite.protocol.ProtocolManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

public class MythicCorePlugin extends JavaPlugin implements MythicPlugin {

    private ServerIdentity serverIdentity;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private StaffChannelService staffChannelService;
    private StaffPresenceService staffPresenceService;
    private PunishmentService punishmentService;
    private PunishmentMenuService punishmentMenuService;
    private ChatControlService chatControlService;
    private ChatColorService chatColorService;
    private RankService rankService;
    private GrantService grantService;
    private GrantFlowService grantFlowService;
    private ChatPromptService chatPromptService;
    private CoreMessages messages;
    private CoreEssentialsService essentialsService;
    private PersistenceGateway persistenceGateway;
    private MythicSchema persistenceSchema;
    private DisplayService displayService;
    private CoreHydrationSink hydrationSink;
    private BroadcastService broadcastService;
    private StaffModeService staffModeService;
    private CoreAuditLog auditLog;
    private SocialService socialService;

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
        net.mythicpvp.suite.packet.PacketSession.getInstance()
                .setRenderer(new net.mythicpvp.suite.packet.BukkitPacketRenderer());
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("core.yml");
        saveResourceIfMissing("staff-channels.yml");
        saveResourceIfMissing("punishments.yml");
        saveResourceIfMissing("scoreboard.yml");
        saveResourceIfMissing("tablist.yml");
        saveResourceIfMissing("nametag.yml");
        saveResourceIfMissing("ranks.yml");
        saveResourceIfMissing("command-blocker.yml");
        saveResourceIfMissing("announcements.yml");
        saveResourceIfMissing("motd.yml");
        saveResourceIfMissing("spawn.yml");
        saveResourceIfMissing("staff-mode.yml");
        saveResourceIfMissing("reports.yml");
        saveResourceIfMissing("welcome.yml");
        serverIdentity = ServerIdentity.fromEnvironment();
        configManager = new ConfigManager(this);
        messages = new CoreMessages(new ConfigText(configManager.getOrCreate("messages"), "messages"));

        auditLog = new CoreAuditLog(this);
        essentialsService = new CoreEssentialsService(messages, auditLog, this);
        commandManager = new CommandManager(this);

        persistenceGateway = createPersistenceGateway();
        rankService = new RankService();
        rankService.setPersistence(persistenceGateway);
        rankService.load(configManager.getOrCreate("ranks"));
        chatPromptService = new ChatPromptService(this);
        grantService = new GrantService(rankService, Clock.systemUTC());
        grantService.setPersistence(persistenceGateway);
        socialService = new SocialService(persistenceGateway, Clock.systemUTC());

        net.mythicpvp.suite.config.MythicConfig menusConfig = configManager.getOrCreate("menus");
        net.mythicpvp.core.rank.RankMenuText rankMenuText =
                new net.mythicpvp.core.rank.RankMenuText(menusConfig);
        grantFlowService = new GrantFlowService(rankService, grantService, chatPromptService, rankMenuText);
        ProtocolManager protocolManager = ProtocolManager.getInstance();
        punishmentService = new PunishmentService(protocolManager, Clock.systemUTC());
        punishmentService.setPersistence(persistenceGateway);
        net.mythicpvp.core.rank.PlayerNameColor playerNameColor =
                new net.mythicpvp.core.rank.PlayerNameColor(rankService, grantService);
        net.mythicpvp.core.punishment.PunishmentEnforcer punishmentEnforcer =
                new net.mythicpvp.core.punishment.PunishmentEnforcer(this, messages, playerNameColor);
        punishmentService.setEnforcer(punishmentEnforcer);
        punishmentService.setPardonListener(punishmentEnforcer::onPardon);
        punishmentService.setPardonNoticeListener(punishmentEnforcer::onPardonNotice);
        punishmentService.setExpiryListener(punishmentEnforcer::onExpiry);
        net.mythicpvp.core.punishment.PunishmentSqlRefresher punishmentRefresher =
                new net.mythicpvp.core.punishment.PunishmentSqlRefresher(punishmentService, getLogger());
        punishmentRefresher.setRemoteEnforcer(punishmentEnforcer::enforceTargetOnly);
        punishmentRefresher.start();
        new net.mythicpvp.core.rank.GrantSqlRefresher(grantService, getLogger()).start();
        new net.mythicpvp.core.social.SocialSqlRefresher(socialService, getLogger()).start();
        net.mythicpvp.core.punishment.PunishmentMenuText menuText =
                new net.mythicpvp.core.punishment.PunishmentMenuText(menusConfig);
        punishmentMenuService = new PunishmentMenuService(
                punishmentService, chatPromptService, Clock.systemUTC(), serverIdentity.id(), menuText);
        seedPunishments(configManager.getOrCreate("punishments"));

        hydrationSink = new CoreHydrationSink(getLogger(), rankService, grantService, punishmentService, socialService);
        persistenceGateway.hydrate(new MainThreadHydrationSink(this, hydrationSink));
        announceServerRegistry();

        getServer().getPluginManager().registerEvents(
                new PunishmentLoginGuard(this, punishmentService, hydrationSink, messages), this);
        net.mythicpvp.core.motd.MotdListener.MotdService motdService =
                new net.mythicpvp.core.motd.MotdListener.MotdService();
        motdService.load(configManager.getOrCreate("motd"));
        getServer().getPluginManager().registerEvents(
                new net.mythicpvp.core.motd.MotdListener(motdService), this);
        net.mythicpvp.core.maintenance.MaintenanceService maintenanceService =
                new net.mythicpvp.core.maintenance.MaintenanceService(getLogger(), getDataFolder());
        getServer().getPluginManager().registerEvents(
                new net.mythicpvp.core.maintenance.MaintenanceLoginGuard(maintenanceService, messages), this);
        commandManager.register(new net.mythicpvp.core.command.MaintenanceCommand(maintenanceService));
        net.mythicpvp.core.security.IpTracker ipTracker =
                new net.mythicpvp.core.security.IpTracker(getLogger(), getDataFolder());
        getServer().getPluginManager().registerEvents(ipTracker, this);
        commandManager.register(new net.mythicpvp.core.command.AltsCommand(ipTracker));
        commandManager.register(new net.mythicpvp.core.command.IpReportCommand(ipTracker));
        commandManager.register(new net.mythicpvp.core.command.SetSpawnCommand(configManager.getOrCreate("spawn")));

        displayService = new DisplayService(this, rankService, grantService, serverIdentity.id());
        displayService.loadTemplates(
                configManager.getOrCreate("tablist"),
                configManager.getOrCreate("scoreboard"));
        rankService.setDisplayRefresher(displayService::applyAll);
        grantService.setDisplayRefresher(displayService::refresh);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(displayService), this);
        net.mythicpvp.suite.scheduler.MythicScheduler.runTimer(
                this,
                displayService::applyAll,
                100L,
                100L);
        net.mythicpvp.core.transfer.ShardRegistry shardRegistry =
                new net.mythicpvp.core.transfer.ShardRegistry(getLogger());
        shardRegistry.subscribe();
        getServer().getServicesManager().register(
                net.mythicpvp.core.transfer.ShardRegistry.class,
                shardRegistry,
                this,
                org.bukkit.plugin.ServicePriority.Normal);
        CoreCompletions.register(commandManager, rankService, punishmentService, shardRegistry);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(chatPromptService, this);
        getServer().getPluginManager().registerEvents(new net.mythicpvp.core.security.OpCommandGuard(), this);
        net.mythicpvp.core.transfer.ProxyTransferService transferService =
                new net.mythicpvp.core.transfer.ProxyTransferService(this);
        String proxyDomain = System.getenv().getOrDefault("MYTHIC_PROXY_DOMAIN", "play.mythicpvp.net");
        int proxyPort = parseInt(System.getenv("MYTHIC_PROXY_PORT"), 25565);
        transferService.setProxyDomain(proxyDomain, proxyPort);
        getLogger().info("[transfer] using proxy domain " + proxyDomain + ":" + proxyPort
                + " for cross-shard transfers");
        getServer().getServicesManager().register(
                net.mythicpvp.core.transfer.ProxyTransferService.class,
                transferService,
                this,
                org.bukkit.plugin.ServicePriority.Normal);
        net.mythicpvp.core.transfer.TransferQueueService transferQueueService =
                new net.mythicpvp.core.transfer.TransferQueueService(
                        this, transferService, rankService, grantService);
        getServer().getServicesManager().register(
                net.mythicpvp.core.transfer.TransferQueueService.class,
                transferQueueService,
                this,
                org.bukkit.plugin.ServicePriority.Normal);
        commandManager.register(new net.mythicpvp.core.command.ServerCommand(transferService, messages));
        commandManager.register(new net.mythicpvp.core.command.HubCommand(
                transferService, messages, serverIdentity.id(), serverIdentity.type(), shardRegistry, getLogger()));
        commandManager.register(new net.mythicpvp.core.command.QueueCommand(transferQueueService));
        displayService.setQueuePositionLookup(transferQueueService::position);
        displayService.setQueueStatusLookup(transferQueueService::statusFor);
        transferQueueService.setOnQueueChange(uuid -> {
            org.bukkit.entity.Player p = getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                net.mythicpvp.suite.scheduler.MythicScheduler.runSync(this, () -> displayService.apply(p));
            }
        });
        commandManager.register(new GrantCommand(grantFlowService));
        commandManager.register(new GrantsCommand(grantService, rankService));
        commandManager.register(new CGrantCommand(grantService));
        commandManager.register(new ClearGrantsCommand(grantService));
        commandManager.register(new RankEditorCommand(rankService, rankMenuText,
                new net.mythicpvp.core.rank.RankEditorMenuService(rankService, chatPromptService, rankMenuText)));
        commandManager.register(new PunishCommand(punishmentMenuService));
        commandManager.register(new PunishmentsCommand(punishmentMenuService));
        commandManager.register(new HistoryCommand(punishmentService, punishmentMenuService));
        commandManager.register(new ClearPunishmentsCommand(punishmentService));
        commandManager.register(new net.mythicpvp.core.command.ClearHistoryCommand(punishmentService));
        commandManager.register(new PunishmentDirectCommand.Ban(punishmentService, serverIdentity.id(), Clock.systemUTC()));
        commandManager.register(new PunishmentDirectCommand.TempBan(punishmentService, serverIdentity.id(), Clock.systemUTC()));
        commandManager.register(new PunishmentDirectCommand.Mute(punishmentService, serverIdentity.id(), Clock.systemUTC()));
        commandManager.register(new PunishmentDirectCommand.TempMute(punishmentService, serverIdentity.id(), Clock.systemUTC()));
        commandManager.register(new PunishmentDirectCommand.Blacklist(punishmentService, serverIdentity.id(), Clock.systemUTC()));
        commandManager.register(new PunishmentDirectCommand.Warn(punishmentService, serverIdentity.id(), Clock.systemUTC()));
        commandManager.register(new PunishmentDirectCommand.Kick(punishmentService, serverIdentity.id(), Clock.systemUTC()));
        commandManager.register(new PunishmentDirectCommand.Unban(punishmentService));
        commandManager.register(new PunishmentAddCommand(punishmentService));
        commandManager.register(new PunishmentRemoveCommand(punishmentService));
        commandManager.register(new PunishmentEditCommand(punishmentService, chatPromptService));
        commandManager.register(new GamemodeCommand(essentialsService));
        commandManager.register(new GmcCommand(essentialsService));
        commandManager.register(new GmsCommand(essentialsService));
        commandManager.register(new TeleportCommand(essentialsService));
        commandManager.register(new TpHereCommand(essentialsService));
        commandManager.register(new HelpCommand(essentialsService));
        commandManager.register(new net.mythicpvp.core.command.ListCommand(rankService, grantService));
        commandManager.register(new DiscordCommand(essentialsService));
        commandManager.register(new FriendCommand(socialService));
        commandManager.register(new PartyCommand(socialService, serverIdentity.id()));
        commandManager.register(new MailCommand(socialService, messages));
        PrivateMessageCommand privateMessages = new PrivateMessageCommand(rankService, grantService);
        commandManager.register(privateMessages);
        commandManager.register(new PrivateMessageCommand.Reply(privateMessages));
        getServer().getPluginManager().registerEvents(new MailLoginListener(socialService, messages), this);
        getServer().getPluginManager().registerEvents(new FriendLoginListener(socialService, messages), this);
        getServer().getPluginManager().registerEvents(new OfflineRewardService(socialService, messages), this);
        staffChannelService = new StaffChannelService(protocolManager, serverIdentity.id());

        String staffFormat = messages.raw(
                "messages.staff.format",
                "&#888888[%server%] %rank_color%%rank%%sender% &8\u00BB &#FFFFFF%message%",
                java.util.Map.of());
        staffChannelService.addAudience(new BukkitStaffAudience(staffFormat));

        commandManager.register(new StaffChatCommand.Staff(staffChannelService, rankService, grantService));
        commandManager.register(new StaffChatCommand.Builder(staffChannelService, rankService, grantService));
        commandManager.register(new StaffChatCommand.Management(staffChannelService, rankService, grantService));
        commandManager.register(new StaffChatCommand.Admin(staffChannelService, rankService, grantService));
        commandManager.register(new StaffChatCommand.Owner(staffChannelService, rankService, grantService));
        getServer().getPluginManager().registerEvents(
                new StaffChatToggleListener(staffChannelService, rankService, grantService), this);
        staffPresenceService = new StaffPresenceService(protocolManager, serverIdentity.id());

        staffPresenceService.addAudience(new BukkitStaffPresenceAudience(this,
                new net.mythicpvp.suite.config.ConfigText(
                        configManager.getOrCreate("messages"), "messages")));
        getServer().getPluginManager().registerEvents(
                new StaffPresenceListener(staffPresenceService, rankService, grantService), this);

        staffModeService = new StaffModeService();
        staffModeService.load(configManager.getOrCreate("staff-mode"));
        staffModeService.configureVisibility(this, rankService, grantService, displayService::applyAll);
        displayService.setStaffModeService(staffModeService);
        commandManager.register(new StaffModeCommand(staffModeService, messages));
        commandManager.register(new VanishCommand(staffModeService));
        commandManager.register(new net.mythicpvp.core.command.FreezeCommand(staffModeService));
        commandManager.register(new net.mythicpvp.core.command.RankCommand(rankService));
        getServer().getPluginManager().registerEvents(
                new StaffModeToolListener(staffModeService, messages), this);

        commandManager.register(new AppealCommand(punishmentService, persistenceGateway, messages, auditLog));
        commandManager.register(new AppealsCommand(persistenceGateway, messages, auditLog));

        RankCosmeticBundles cosmeticBundles = new RankCosmeticBundles();
        cosmeticBundles.load(configManager.getOrCreate("ranks"));
        grantService.setGrantObserver(new RankBundleGrantHook(
                cosmeticBundles, auditLog, getLogger(), persistenceGateway));

        saveResourceIfMissing("cosmetics.yml");
        saveResourceIfMissing("crates.yml");
        net.mythicpvp.core.cosmetic.CosmeticCatalogLoader catalogLoader =
                new net.mythicpvp.core.cosmetic.CosmeticCatalogLoader(configManager.getOrCreate("cosmetics"), getLogger());
        catalogLoader.load();
        net.mythicpvp.core.cosmetic.CosmeticService cosmeticService =
                new net.mythicpvp.core.cosmetic.CosmeticService(persistenceGateway, this);
        net.mythicpvp.core.cosmetic.CrateService crateService =
                new net.mythicpvp.core.cosmetic.CrateService(cosmeticService, getLogger());
        crateService.loadFromConfig(configManager.getOrCreate("crates"));
        net.mythicpvp.core.cosmetic.CosmeticMenuText cosmeticMenuText =
                new net.mythicpvp.core.cosmetic.CosmeticMenuText(menusConfig);
        net.mythicpvp.core.cosmetic.CosmeticMenuService cosmeticMenuService =
                new net.mythicpvp.core.cosmetic.CosmeticMenuService(cosmeticService, crateService, cosmeticMenuText);
        commandManager.register(new net.mythicpvp.core.command.CosmeticsCommand(cosmeticMenuService));
        getServer().getPluginManager().registerEvents(
                new net.mythicpvp.core.cosmetic.CosmeticRedeemListener(cosmeticService), this);

        saveResourceIfMissing("creditshop.yml");
        net.mythicpvp.core.credit.CreditService creditService = new net.mythicpvp.core.credit.CreditService();
        net.mythicpvp.core.credit.CreditShopText creditShopText =
                new net.mythicpvp.core.credit.CreditShopText(menusConfig);
        net.mythicpvp.core.credit.CreditShopService creditShopService =
                new net.mythicpvp.core.credit.CreditShopService(
                        creditService, grantService, cosmeticService, crateService, creditShopText);
        creditShopService.loadFromConfig(configManager.getOrCreate("creditshop"));
        commandManager.register(new net.mythicpvp.core.command.CreditsCommand(creditService));
        commandManager.register(new net.mythicpvp.core.command.CreditShopCommand(creditShopService));

        chatControlService = new ChatControlService(protocolManager, serverIdentity.id());
        saveResourceIfMissing("chat-filters.yml");
        net.mythicpvp.core.chat.ChatFilterService chatFilterService = new net.mythicpvp.core.chat.ChatFilterService(
                configManager.getOrCreate("chat-filters"), punishmentService, Clock.systemUTC(), serverIdentity.id());
        chatFilterService.load();
        net.mythicpvp.core.chat.ChatFilterMenu chatFilterMenu =
                new net.mythicpvp.core.chat.ChatFilterMenu(chatFilterService, chatPromptService);
        commandManager.register(new net.mythicpvp.core.chat.ChatFilterCommand(chatFilterService));
        commandManager.register(new net.mythicpvp.core.chat.ChatFilterCommand.FiltersCommand(chatFilterMenu));
        ChatGuard chatGuard = new ChatGuard(this, chatControlService, punishmentService, messages,
                serverIdentity.id(), chatFilterService);
        getServer().getPluginManager().registerEvents(chatGuard, this);
        chatColorService = new ChatColorService();
        getServer().getPluginManager().registerEvents(
                new ChatFormatListener(rankService, grantService, configManager.getOrCreate("core"), chatColorService),
                this);
        ChatColorMenuService chatColorMenuService = new ChatColorMenuService(chatColorService);
        commandManager.register(new net.mythicpvp.core.command.ChatColorCommand(chatColorMenuService));
        commandManager.register(new net.mythicpvp.core.command.CcCommand(chatColorMenuService));
        commandManager.register(new ChatCommand(chatControlService, messages));
        commandManager.register(new net.mythicpvp.core.command.UnmuteCommand(punishmentService));

        net.mythicpvp.core.report.ReportService reportService =
                new net.mythicpvp.core.report.ReportService();
        reportService.setStore(new net.mythicpvp.core.report.ReportStore(
                new java.io.File(getDataFolder(), "reports-data.yml"), getLogger()));
        net.mythicpvp.core.report.ReportMenuService reportMenuService =
                new net.mythicpvp.core.report.ReportMenuService(reportService, chatPromptService, serverIdentity.id());
        net.mythicpvp.core.command.ReportConfig reportConfig =
                new net.mythicpvp.core.command.ReportConfig(configManager.getOrCreate("reports"));
        commandManager.register(new net.mythicpvp.core.command.ReportCommand(
                reportService, reportMenuService, reportConfig, serverIdentity.id()));
        commandManager.register(new net.mythicpvp.core.command.ReportsCommand(reportMenuService));
        commandManager.register(new net.mythicpvp.core.command.HelpopCommand(reportConfig, serverIdentity.id()));
        commandManager.register(new net.mythicpvp.core.command.RequestCommand(reportConfig, serverIdentity.id()));

        net.mythicpvp.core.note.NoteService noteService = new net.mythicpvp.core.note.NoteService();
        noteService.setStore(new net.mythicpvp.core.note.NoteStore(
                new java.io.File(getDataFolder(), "notes-data.yml"), getLogger()));
        net.mythicpvp.core.note.NoteMenuService noteMenuService =
                new net.mythicpvp.core.note.NoteMenuService(noteService);
        commandManager.register(new net.mythicpvp.core.command.NotesCommand(noteService, noteMenuService));
        commandManager.register(new net.mythicpvp.core.command.NoteCommand(
                noteService, chatPromptService, serverIdentity.id()));

        net.mythicpvp.core.punishment.ManagePunishmentsMenuService managePunishmentsMenu =
                new net.mythicpvp.core.punishment.ManagePunishmentsMenuService(
                        punishmentService, noteService, chatPromptService);
        commandManager.register(new net.mythicpvp.core.command.ManagePunishmentsCommand(
                managePunishmentsMenu, punishmentService));
        commandManager.register(new net.mythicpvp.core.command.ClearInvCommand());
        commandManager.register(new net.mythicpvp.core.command.CiCommand());

        net.mythicpvp.core.session.SessionTracker sessionTracker = new net.mythicpvp.core.session.SessionTracker();
        getServer().getPluginManager().registerEvents(sessionTracker, this);
        net.mythicpvp.core.staff.StaffListMenuService staffListMenu =
                new net.mythicpvp.core.staff.StaffListMenuService(rankService, grantService, sessionTracker);
        commandManager.register(new net.mythicpvp.core.command.StaffListCommand(staffListMenu));

        net.mythicpvp.core.staffmode.StaffSettings staffSettings =
                new net.mythicpvp.core.staffmode.StaffSettings(
                        new java.io.File(getDataFolder(), "staff-settings.yml"), getLogger());
        net.mythicpvp.core.staffmode.StaffStateStore staffStateStore =
                new net.mythicpvp.core.staffmode.StaffStateStore(
                        new java.io.File(getDataFolder(), "staff-state.yml"), getLogger());
        net.mythicpvp.core.staffmode.StaffSettingsMenuService staffSettingsMenu =
                new net.mythicpvp.core.staffmode.StaffSettingsMenuService(staffSettings);
        commandManager.register(new net.mythicpvp.core.command.StaffSettingsCommand(staffSettingsMenu));
        getServer().getPluginManager().registerEvents(
                new net.mythicpvp.core.staffmode.StaffModeJoinHandler(
                        this, staffModeService, staffStateStore, staffSettings,
                        rankService, grantService, staffChannelService),
                this);

        net.mythicpvp.core.welcome.WelcomeService welcomeService =
                new net.mythicpvp.core.welcome.WelcomeService(configManager.getOrCreate("welcome"));
        getServer().getPluginManager().registerEvents(
                new net.mythicpvp.core.welcome.WelcomeListener(this, welcomeService), this);

        broadcastService = new BroadcastService(protocolManager, serverIdentity.id());
        broadcastService.load(configManager.getOrCreate("announcements"));
        commandManager.register(new BroadcastCommand(broadcastService));
        if (broadcastService.enabled() && broadcastService.announcementCount() > 0) {
            long periodTicks = broadcastService.intervalSeconds() * 20L;

            net.mythicpvp.suite.scheduler.MythicScheduler.runTimer(
                    this,
                    () -> {
                        try {
                            broadcastService.tickAnnouncement();
                        } catch (RuntimeException e) {
                            getLogger().warning("announcement tick failed: " + e.getMessage());
                        }
                    },
                    periodTicks,
                    periodTicks);
        }
    }

    @Override
    public void disable() {

        if (persistenceSchema != null && serverIdentity != null) {
            try {
                persistenceSchema.registryHeartbeat(
                        serverIdentity.id(),
                        ServerStatus.OFFLINE,
                        0,
                        0.0f,
                        0.0f);
            } catch (RuntimeException e) {
                getLogger().warning("[stdb] final OFFLINE heartbeat failed: " + e.getMessage());
            }
        }

        if (configManager != null) {
            try {
                configManager.saveAll();
            } catch (RuntimeException e) {
                getLogger().warning("config save during disable failed: " + e.getMessage());
            }
        }

        try {
            net.mythicpvp.suite.tab.TabManager.getInstance().clear();
            net.mythicpvp.suite.nametag.NametagManager.getInstance().clear();
            net.mythicpvp.suite.scoreboard.BoardManager.getInstance().removeAll();
            net.mythicpvp.suite.packet.PacketSession.getInstance().clear();
        } catch (RuntimeException e) {
            getLogger().warning("UI manager teardown failed: " + e.getMessage());
        }

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
    public ChatColorService chatColorService() {
        return chatColorService;
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

    @NotNull
    private PersistenceGateway createPersistenceGateway() {
        String uri = System.getenv("STDB_URI");
        String module = System.getenv().getOrDefault("STDB_MODULE", "mythicpvp");
        if (uri == null || uri.isBlank()) {
            getLogger().info("STDB_URI not set - mythic-core running in single-server / no-op persistence mode");
            return NoopPersistenceGateway.INSTANCE;
        }
        try {
            SpacetimeConnection connection = DatabaseManager.getInstance()
                    .createConnection("mythic-core", uri, module);

            connection.connect().get(10, TimeUnit.SECONDS);
            MythicSchema schema = new MythicSchema(connection);
            persistenceSchema = schema;
            getLogger().info("STDB persistence active: uri=" + uri + " module=" + module);
            return new StdbPersistenceGateway(getLogger(), schema, connection);
        } catch (Exception failure) {
            getLogger().warning("Failed to construct STDB connection (" + failure.getMessage()
                    + "); falling back to no-op persistence");
            return NoopPersistenceGateway.INSTANCE;
        }
    }

    private void announceServerRegistry() {
        if (persistenceSchema == null) {
            return;
        }
        ServerRole role = ServerRole.fromWire(serverIdentity.type().toUpperCase(java.util.Locale.ROOT));
        if (role == null) {
            getLogger().warning("[stdb] registry announce skipped: unknown server type " + serverIdentity.type());
            return;
        }
        String address = firstPresent("MYTHIC_ADDRESS", "SERVER_ADDRESS", "PUBLIC_ADDRESS");
        if (address == null || address.isBlank()) {
            String host = firstPresent("SERVER_IP", "P_SERVER_IP");
            String port = firstPresent("SERVER_PORT", "P_SERVER_PORT");
            if (host != null && !host.isBlank() && port != null && !port.isBlank()) {
                address = host + ":" + port;
            }
        }
        if (address == null || address.isBlank()) {
            address = serverIdentity.id() + ":25565";
        }
        int maxPlayers = parseInt(firstPresent("MAX_PLAYERS", "SERVER_MAX_PLAYERS"), 100);
        String region = java.util.Optional.ofNullable(firstPresent("MYTHIC_REGION", "SERVER_REGION"))
                .filter(s -> !s.isBlank())
                .orElse("vps");

        persistenceSchema.registryAnnounce(
                serverIdentity.id(),
                role,
                region,
                address,
                maxPlayers,
                SchemaVersion.CURRENT);
        sendHeartbeat();
        getLogger().info("[stdb] registry announced shard=" + serverIdentity.id()
                + " role=" + role.wireValue() + " address=" + address);

        long heartbeatPeriodTicks = 15L * 20L;
        net.mythicpvp.suite.scheduler.MythicScheduler.runTimer(
                this,
                () -> {
                    try {
                        sendHeartbeat();
                    } catch (RuntimeException e) {
                        getLogger().warning("[stdb] heartbeat tick failed: " + e.getMessage());
                    }
                },
                heartbeatPeriodTicks,
                heartbeatPeriodTicks);
    }

    private void sendHeartbeat() {
        if (persistenceSchema == null) {
            return;
        }
        persistenceSchema.registryHeartbeat(
                serverIdentity.id(),
                ServerStatus.HEALTHY,
                getServer().getOnlinePlayers().size(),
                20.0f,
                0.0f);
    }

    private static String firstPresent(@NotNull String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (value == null || value.isBlank()) {
                value = System.getProperty(key);
            }
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
