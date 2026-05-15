package net.mythicpvp.hub.activity;

import net.mythicpvp.suite.config.MythicConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public final class HubActivityService {

    private boolean doubleJumpEnabled;
    private double doubleJumpVelocity;
    private double doubleJumpUpward;

    private boolean launchPadsEnabled;
    private double launchPadVelocity;
    private double launchPadUpward;
    private Material launchPadMaterial;

    public void load(@NotNull MythicConfig config) {
        doubleJumpEnabled = config.getBoolean("activities.double-jump.enabled", true);
        doubleJumpVelocity = config.getDouble("activities.double-jump.velocity", 1.5);
        doubleJumpUpward = config.getDouble("activities.double-jump.upward", 0.8);

        launchPadsEnabled = config.getBoolean("activities.launch-pads.enabled", true);
        launchPadVelocity = config.getDouble("activities.launch-pads.velocity", 2.5);
        launchPadUpward = config.getDouble("activities.launch-pads.upward", 1.0);
        String matName = config.getString("activities.launch-pads.material", "SLIME_BLOCK");
        try {
            launchPadMaterial = Material.valueOf(matName);
        } catch (IllegalArgumentException e) {
            launchPadMaterial = Material.SLIME_BLOCK;
        }
    }

    public void applyDoubleJump(@NotNull Player player) {
        if (!doubleJumpEnabled) return;
        Vector direction = player.getLocation().getDirection().normalize()
                .multiply(doubleJumpVelocity)
                .setY(doubleJumpUpward);
        player.setVelocity(direction);
        player.setAllowFlight(true);
        player.setFlying(false);
    }

    public void applyLaunchPad(@NotNull Player player) {
        if (!launchPadsEnabled) return;
        Vector direction = player.getLocation().getDirection().normalize()
                .multiply(launchPadVelocity)
                .setY(launchPadUpward);
        player.setVelocity(direction);
    }

    public void enableFlight(@NotNull Player player) {
        if (doubleJumpEnabled) {
            player.setAllowFlight(true);
        }
    }

    public boolean isDoubleJumpEnabled() {
        return doubleJumpEnabled;
    }

    public boolean isLaunchPadsEnabled() {
        return launchPadsEnabled;
    }

    @NotNull
    public Material getLaunchPadMaterial() {
        return launchPadMaterial;
    }
}
