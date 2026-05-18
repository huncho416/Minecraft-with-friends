package net.mythicpvp.core.command;

import net.mythicpvp.core.prompt.ChatPromptService;
import net.mythicpvp.core.rank.CoreRank;
import net.mythicpvp.core.rank.RankService;
import net.mythicpvp.suite.disguise.DisguiseManager;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.item.MythicItem;
import net.mythicpvp.suite.menu.MythicMenu;
import net.mythicpvp.suite.menu.PaginatedMenu;
import net.mythicpvp.suite.scheduler.MythicScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DisguiseMenuService {

    private final JavaPlugin plugin;
    private final RankService ranks;
    private final ChatPromptService prompts;
    private final Map<UUID, PendingDisguise> sessions = new ConcurrentHashMap<>();
    private final Map<String, SkinPreset> skinPresets = new LinkedHashMap<>();
    private final List<String> namePresets;

    public DisguiseMenuService(@NotNull JavaPlugin plugin,
                               @NotNull RankService ranks,
                               @NotNull ChatPromptService prompts) {
        this.plugin = plugin;
        this.ranks = ranks;
        this.prompts = prompts;
        this.namePresets = List.copyOf(DisguiseManager.getInstance().getRandomNames());
        seedDefaultSkinPresets();
    }

    public void registerSkinPreset(@NotNull String id, @NotNull String displayName,
                                   @NotNull String sourceName,
                                   @Nullable String value, @Nullable String signature) {
        skinPresets.put(id.toLowerCase(Locale.ROOT),
                new SkinPreset(id, displayName, sourceName, value, signature));
    }

    public void openMain(@NotNull Player player) {
        sessions.put(player.getUniqueId(), new PendingDisguise());
        openRankPicker(player);
    }

    public void openRankPicker(@NotNull Player player) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#FF00F8Disguise &8» &#D2D8E0Rank");

        menu.addItem(MythicItem.create(Material.BARRIER)
                .name("&#FF8A8ANo rank override")
                .lore(List.of(
                        "&7Keep your real rank visible.",
                        "&#D2D8E0Click to choose"))
                .build(), event -> {
            session(player).rankId = null;
            session(player).rankDisplay = "No override";
            openSkinPicker(player);
        });

        for (CoreRank rank : ranks.all()) {
            CoreRank r = rank;
            menu.addItem(MythicItem.create(rank.dye())
                    .name(MythicHex.normalizeBareHex(rank.prefix()) + " " + rank.name())
                    .lore(List.of(
                            "&7Weight: &f" + rank.weight(),
                            "&7Staff: &f" + (rank.staff() ? "Yes" : "No"),
                            "&#D2D8E0Click to pick"))
                    .build(), event -> {
                session(player).rankId = r.id();
                session(player).rankDisplay = r.name();
                openSkinPicker(player);
            });
        }

        menu.staticSlot(49, MythicItem.create(Material.RED_WOOL)
                .name("&#FF8A8ACancel")
                .build(), event -> close(player));

        menu.open(player);
    }

    public void openSkinPicker(@NotNull Player player) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#FF00F8Disguise &8» &#D2D8E0Skin");

        menu.addItem(MythicItem.create(Material.BARRIER)
                .name("&#FF8A8ANo skin override")
                .lore(List.of(
                        "&7Use your real skin.",
                        "&#D2D8E0Click to choose"))
                .build(), event -> {
            session(player).skinValue = null;
            session(player).skinSignature = null;
            session(player).skinSource = "Your skin";
            openNamePicker(player);
        });

        menu.addItem(MythicItem.create(Material.WRITABLE_BOOK)
                .name("&#9CFF9CType a player's name…")
                .lore(List.of(
                        "&7Wear the skin of any online or",
                        "&7premium Minecraft account.",
                        "&#D2D8E0Click and enter the name in chat"))
                .build(), event -> {
            prompts.await(player, (p, input) -> resolveSkinFromName(p, input.trim()));
        });

        for (SkinPreset preset : skinPresets.values()) {
            SkinPreset chosen = preset;
            menu.addItem(MythicItem.create(Material.PLAYER_HEAD)
                    .name("&#F529BE" + preset.displayName)
                    .lore(List.of(
                            "&7Skin of &f" + preset.sourceName,
                            "&#D2D8E0Click to pick"))
                    .build(), event -> {
                session(player).skinValue = chosen.value;
                session(player).skinSignature = chosen.signature;
                session(player).skinSource = chosen.displayName;
                openNamePicker(player);
            });
        }

        menu.staticSlot(45, MythicItem.create(Material.ARROW)
                .name("&#FF00F8Back to rank")
                .build(), event -> openRankPicker(player));
        menu.staticSlot(49, MythicItem.create(Material.RED_WOOL)
                .name("&#FF8A8ACancel")
                .build(), event -> close(player));

        menu.open(player);
    }

    public void openNamePicker(@NotNull Player player) {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#FF00F8Disguise &8» &#D2D8E0Name");

        menu.addItem(MythicItem.create(Material.WRITABLE_BOOK)
                .name("&#9CFF9CType a custom name…")
                .lore(List.of(
                        "&7Enter the display name in chat.",
                        "&#D2D8E0Click and enter in chat"))
                .build(), event -> {
            prompts.await(player, (p, input) -> {
                String trimmed = input.trim();
                if (trimmed.length() < 2 || trimmed.length() > 16) {
                    p.sendMessage(MythicHex.colorize(
                            "&#FF8A8AName must be 2-16 characters. Pick another."));
                    openNamePicker(p);
                    return;
                }
                session(p).displayName = trimmed;
                openConfirmation(p);
            });
        });

        for (String preset : namePresets) {
            String chosen = preset;
            menu.addItem(MythicItem.create(Material.NAME_TAG)
                    .name("&#F529BE" + preset)
                    .lore(List.of("&#D2D8E0Click to pick"))
                    .build(), event -> {
                session(player).displayName = chosen;
                openConfirmation(player);
            });
        }

        menu.staticSlot(45, MythicItem.create(Material.ARROW)
                .name("&#FF00F8Back to skin")
                .build(), event -> openSkinPicker(player));
        menu.staticSlot(49, MythicItem.create(Material.RED_WOOL)
                .name("&#FF8A8ACancel")
                .build(), event -> close(player));

        menu.open(player);
    }

    public void openConfirmation(@NotNull Player player) {
        PendingDisguise pending = session(player);
        if (pending.displayName == null) {
            player.sendMessage(MythicHex.colorize("&#FF8A8APick a display name first."));
            openNamePicker(player);
            return;
        }

        MythicMenu menu = MythicMenu.create(3, "&#FF00F8Disguise &8» &#D2D8E0Confirm");

        menu.slot(13, MythicItem.create(Material.BOOK)
                .name("&#F529BEDisguise summary")
                .lore(List.of(
                        "&7Name: &f" + pending.displayName,
                        "&7Rank: &f" + (pending.rankDisplay == null ? "Your rank" : pending.rankDisplay),
                        "&7Skin: &f" + (pending.skinSource == null ? "Your skin" : pending.skinSource)))
                .build());

        menu.slot(11, MythicItem.create(Material.LIME_WOOL)
                .name("&#9CFF9CApply disguise")
                .lore(List.of("&7Click to apply"))
                .build(), event -> apply(player));

        menu.slot(15, MythicItem.create(Material.RED_WOOL)
                .name("&#FF8A8ACancel")
                .lore(List.of("&7Discard this disguise"))
                .build(), event -> close(player));

        menu.open(player);
    }

    private void apply(@NotNull Player player) {
        PendingDisguise pending = session(player);
        if (pending.displayName == null) {
            return;
        }
        DisguiseManager.SkinProperties skin = null;
        if (pending.skinValue != null && pending.skinSignature != null) {
            skin = new DisguiseManager.SkinProperties(pending.skinValue, pending.skinSignature);
        }
        DisguiseManager.getInstance().disguiseAs(
                player.getUniqueId(), pending.displayName, skin, pending.rankId);
        sessions.remove(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(MythicHex.colorize(
                "&#9CFF9CDisguised as &#FFFFFF" + pending.displayName + "&#9CFF9C."));
    }

    private void close(@NotNull Player player) {
        sessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    private void resolveSkinFromName(@NotNull Player requester, @NotNull String name) {
        if (name.isBlank() || name.length() > 16) {
            requester.sendMessage(MythicHex.colorize(
                    "&#FF8A8ANot a valid Minecraft name. Pick another."));
            openSkinPicker(requester);
            return;
        }
        requester.sendMessage(MythicHex.colorize(
                "&#D2D8E0Fetching skin for &#FFFFFF" + name + "&#D2D8E0…"));
        MythicScheduler.runAsync(plugin, () -> {
            try {
                PlayerProfile profile = Bukkit.createProfile(name);
                profile = profile.update().get();
                if (profile.getUniqueId() == null) {
                    MythicScheduler.runSync(plugin, () -> {
                        requester.sendMessage(MythicHex.colorize(
                                "&#FF8A8AUnknown player &#FFFFFF" + name + "&#FF8A8A."));
                        openSkinPicker(requester);
                    });
                    return;
                }
                PlayerTextures textures = profile.getTextures();
                URL skinUrl = textures.getSkin();
                if (skinUrl == null) {
                    MythicScheduler.runSync(plugin, () -> {
                        requester.sendMessage(MythicHex.colorize(
                                "&#FF8A8AThat player has no skin set."));
                        openSkinPicker(requester);
                    });
                    return;
                }
                String resolvedName = profile.getName() == null ? name : profile.getName();
                String value = encodeTextureValue(skinUrl.toString());
                MythicScheduler.runSync(plugin, () -> {
                    PendingDisguise pd = session(requester);
                    pd.skinValue = value;
                    pd.skinSignature = "";
                    pd.skinSource = resolvedName + "'s skin";
                    openNamePicker(requester);
                });
            } catch (Exception ex) {
                MythicScheduler.runSync(plugin, () -> {
                    requester.sendMessage(MythicHex.colorize(
                            "&#FF8A8ASkin lookup failed: " + ex.getClass().getSimpleName()));
                    openSkinPicker(requester);
                });
            }
        });
    }

    @NotNull
    private static String encodeTextureValue(@NotNull String skinUrl) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + skinUrl + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @NotNull
    private PendingDisguise session(@NotNull Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new PendingDisguise());
    }

    private void seedDefaultSkinPresets() {
        registerSkinPreset("notch", "Notch", "Notch", null, null);
        registerSkinPreset("steve", "Steve", "Steve", null, null);
        registerSkinPreset("alex", "Alex", "Alex", null, null);
    }

    private static final class PendingDisguise {
        @Nullable String rankId;
        @Nullable String rankDisplay;
        @Nullable String skinValue;
        @Nullable String skinSignature;
        @Nullable String skinSource;
        @Nullable String displayName;
    }

    private record SkinPreset(@NotNull String id,
                              @NotNull String displayName,
                              @NotNull String sourceName,
                              @Nullable String value,
                              @Nullable String signature) {}
}
