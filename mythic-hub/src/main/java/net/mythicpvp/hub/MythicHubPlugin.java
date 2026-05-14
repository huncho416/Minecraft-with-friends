package net.mythicpvp.hub;

import net.mythicpvp.hub.activity.HubActivityListener;
import net.mythicpvp.hub.activity.HubActivityService;
import net.mythicpvp.hub.command.ServerSelectorCommand;
import net.mythicpvp.hub.selector.ServerSelectorMenu;
import net.mythicpvp.hub.selector.ServerSelectorService;
import net.mythicpvp.hub.spawn.SpawnListener;
import net.mythicpvp.hub.spawn.SpawnService;
import net.mythicpvp.suite.api.MythicPlugin;
import net.mythicpvp.suite.command.CommandManager;
import net.mythicpvp.suite.config.ConfigManager;
import net.mythicpvp.suite.config.MythicConfig;
import net.mythicpvp.suite.menu.MenuListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class MythicHubPlugin extends JavaPlugin implements MythicPlugin {

    private ConfigManager configManager;
    private SpawnService spawnService;
    private ServerSelectorService selectorService;
    private HubActivityService activityService;

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
        saveResourceIfMissing("hub.yml");
        configManager = new ConfigManager(this);
        MythicConfig hubConfig = configManager.getOrCreate("hub");

        spawnService = new SpawnService(this);
        spawnService.load(hubConfig);

        selectorService = new ServerSelectorService();
        selectorService.loadGroups(hubConfig);

        activityService = new HubActivityService();
        activityService.load(hubConfig);

        CommandManager commandManager = new CommandManager(this);
        ServerSelectorMenu selectorMenu = new ServerSelectorMenu(selectorService);
        commandManager.register(new ServerSelectorCommand(selectorMenu));

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnListener(spawnService), this);
        getServer().getPluginManager().registerEvents(new HubActivityListener(activityService), this);
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
    }

    @NotNull
    public SpawnService getSpawnService() {
        return spawnService;
    }

    @NotNull
    public ServerSelectorService getSelectorService() {
        return selectorService;
    }

    @NotNull
    public HubActivityService getActivityService() {
        return activityService;
    }

    @Override
    @NotNull
    public String getServerIdentifier() {
        return "hub";
    }

    private void saveResourceIfMissing(@NotNull String name) {
        if (!new java.io.File(getDataFolder(), name).exists()) {
            saveResource(name, false);
        }
    }
}
