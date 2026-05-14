package net.mythicpvp.core;

import net.mythicpvp.core.chat.ChatControlService;
import net.mythicpvp.core.command.CGrantCommand;
import net.mythicpvp.core.command.ClearGrantsCommand;
import net.mythicpvp.core.command.CoreCompletions;
import net.mythicpvp.core.command.GrantCommand;
import net.mythicpvp.core.command.GrantsCommand;
import net.mythicpvp.core.command.RankEditorCommand;
import net.mythicpvp.core.config.CoreMessages;
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
import net.mythicpvp.suite.menu.MenuListener;
import net.mythicpvp.suite.protocol.ProtocolManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;

public final class MythicCorePlugin extends JavaPlugin implements MythicPlugin {

    private ServerIdentity serverIdentity;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private StaffChannelService staffChannelService;
    private StaffPresenceService staffPresenceService;
    private PunishmentService punishmentService;
    private ChatControlService chatControlService;
    private RankService rankService;
    private GrantService grantService;
    private GrantFlowService grantFlowService;
    private ChatPromptService chatPromptService;
    private CoreMessages messages;

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
        saveResourceIfMissing("ranks.yml");
        serverIdentity = ServerIdentity.fromEnvironment();
        configManager = new ConfigManager(this);
        messages = new CoreMessages(new ConfigText(configManager.getOrCreate("messages"), "messages"));
        commandManager = new CommandManager(this);
        rankService = new RankService();
        rankService.load(configManager.getOrCreate("ranks"));
        chatPromptService = new ChatPromptService(this);
        grantService = new GrantService(rankService, Clock.systemUTC());
        grantFlowService = new GrantFlowService(rankService, grantService, chatPromptService);
        CoreCompletions.register(commandManager, rankService);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(chatPromptService, this);
        commandManager.register(new GrantCommand(grantFlowService));
        commandManager.register(new GrantsCommand(grantService, rankService));
        commandManager.register(new CGrantCommand(grantService));
        commandManager.register(new ClearGrantsCommand(grantService));
        commandManager.register(new RankEditorCommand(rankService));
        ProtocolManager protocolManager = ProtocolManager.getInstance();
        staffChannelService = new StaffChannelService(protocolManager, serverIdentity.id());
        staffPresenceService = new StaffPresenceService(protocolManager, serverIdentity.id());
        punishmentService = new PunishmentService(protocolManager, Clock.systemUTC());
        chatControlService = new ChatControlService(protocolManager);
    }

    @Override
    public void disable() {
        if (configManager != null) {
            configManager.saveAll();
        }
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

    private void saveResourceIfMissing(@NotNull String path) {
        if (!getDataFolder().toPath().resolve(path).toFile().exists()) {
            saveResource(path, false);
        }
    }
}
