package net.mythicpvp.core.staffmode;

import net.mythicpvp.core.staff.StaffChannel;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StaffSettings {

    public static final String BYPASS_PERMISSION = "mythic.core.staffsettings.bypass";

    private final File file;
    private final Logger logger;

    private volatile boolean forceStaffModeOnJoin;
    private volatile boolean forceVanishOnJoin;
    private volatile boolean forceStaffChatOnJoin;
    private volatile StaffChannel forcedChannel = StaffChannel.STAFF;

    public StaffSettings(@NotNull File file, @NotNull Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    public boolean forceStaffModeOnJoin() {
        return forceStaffModeOnJoin;
    }

    public void setForceStaffModeOnJoin(boolean value) {
        this.forceStaffModeOnJoin = value;
        save();
    }

    public boolean forceVanishOnJoin() {
        return forceVanishOnJoin;
    }

    public void setForceVanishOnJoin(boolean value) {
        this.forceVanishOnJoin = value;
        save();
    }

    public boolean forceStaffChatOnJoin() {
        return forceStaffChatOnJoin;
    }

    public void setForceStaffChatOnJoin(boolean value) {
        this.forceStaffChatOnJoin = value;
        save();
    }

    @NotNull
    public StaffChannel forcedChannel() {
        return forcedChannel;
    }

    public void setForcedChannel(@NotNull StaffChannel channel) {
        this.forcedChannel = channel;
        save();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        forceStaffModeOnJoin = yaml.getBoolean("force-staff-mode-on-join", false);
        forceVanishOnJoin = yaml.getBoolean("force-vanish-on-join", false);
        forceStaffChatOnJoin = yaml.getBoolean("force-staff-chat-on-join", false);
        String channelId = yaml.getString("forced-channel", "staff");
        for (StaffChannel ch : StaffChannel.values()) {
            if (ch.id().equalsIgnoreCase(channelId)) {
                forcedChannel = ch;
                break;
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("force-staff-mode-on-join", forceStaffModeOnJoin);
        yaml.set("force-vanish-on-join", forceVanishOnJoin);
        yaml.set("force-staff-chat-on-join", forceStaffChatOnJoin);
        yaml.set("forced-channel", forcedChannel.id());
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save staff-settings.yml", e);
        }
    }
}
