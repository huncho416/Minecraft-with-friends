package net.mythicpvp.suite.item;

import net.kyori.adventure.text.Component;
import net.mythicpvp.suite.hex.MythicHex;
import net.mythicpvp.suite.resourcepack.ResourcePackManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MythicItem {

    private final ItemStack itemStack;

    private MythicItem(@NotNull Material material) {
        this.itemStack = new ItemStack(material);
    }

    private MythicItem(@NotNull ItemStack itemStack) {
        this.itemStack = itemStack.clone();
    }

    @NotNull
    public static MythicItem create(@NotNull Material material) {
        return new MythicItem(material);
    }

    @NotNull
    public static MythicItem from(@NotNull ItemStack itemStack) {
        return new MythicItem(itemStack);
    }

    @NotNull
    public MythicItem name(@NotNull String name) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(MythicHex.colorize(name)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        itemStack.setItemMeta(meta);
        return this;
    }

    @NotNull
    public MythicItem lore(@NotNull String... lines) {
        ItemMeta meta = itemStack.getItemMeta();
        List<Component> lore = Arrays.stream(lines)
                .map(MythicHex::colorize)
                .map(c -> c.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
        meta.lore(lore);
        itemStack.setItemMeta(meta);
        return this;
    }

    @NotNull
    public MythicItem lore(@NotNull List<String> lines) {
        return lore(lines.toArray(new String[0]));
    }

    @NotNull
    public MythicItem addLore(@NotNull String line) {
        ItemMeta meta = itemStack.getItemMeta();
        List<Component> existing = meta.lore();
        if (existing == null) existing = new ArrayList<>();
        existing.add(MythicHex.colorize(line));
        meta.lore(existing);
        itemStack.setItemMeta(meta);
        return this;
    }

    @NotNull
    public MythicItem amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    @NotNull
    public MythicItem enchant(@NotNull Enchantment enchantment, int level) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addEnchant(enchantment, level, true);
        itemStack.setItemMeta(meta);
        return this;
    }

    @NotNull
    public MythicItem glow() {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        itemStack.setItemMeta(meta);
        return this;
    }

    @NotNull
    public MythicItem flags(@NotNull ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addItemFlags(flags);
        itemStack.setItemMeta(meta);
        return this;
    }

    @NotNull
    public MythicItem hideAll() {
        return flags(ItemFlag.values());
    }

    @NotNull
    public MythicItem unbreakable() {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setUnbreakable(true);
        itemStack.setItemMeta(meta);
        return this;
    }

    @NotNull
    public MythicItem itemModel(@NotNull NamespacedKey itemModel) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setItemModel(itemModel);
        itemStack.setItemMeta(meta);
        return this;
    }

    @NotNull
    public MythicItem model(@NotNull String modelId) {
        ResourcePackManager.CustomModel model = ResourcePackManager.getInstance().getModel(modelId);
        if (model == null) {
            throw new IllegalArgumentException("Unknown custom model: " + modelId);
        }
        if (itemStack.getType() != model.material()) {
            throw new IllegalArgumentException("Model " + modelId + " requires " + model.material());
        }
        return itemModel(model.itemModel());
    }

    @NotNull
    public <T, Z> MythicItem persistentData(@NotNull JavaPlugin plugin, @NotNull String key, @NotNull PersistentDataType<T, Z> type, @NotNull Z value) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), type, value);
        itemStack.setItemMeta(meta);
        return this;
    }

    @Nullable
    public <T, Z> Z getPersistentData(@NotNull JavaPlugin plugin, @NotNull String key, @NotNull PersistentDataType<T, Z> type) {
        ItemMeta meta = itemStack.getItemMeta();
        return meta.getPersistentDataContainer().get(new NamespacedKey(plugin, key), type);
    }

    public boolean hasPersistentData(@NotNull JavaPlugin plugin, @NotNull String key, @NotNull PersistentDataType<?, ?> type) {
        ItemMeta meta = itemStack.getItemMeta();
        return meta.getPersistentDataContainer().has(new NamespacedKey(plugin, key), type);
    }

    @NotNull
    public MythicItem skullTexture(@NotNull String base64) {
        if (itemStack.getType() != Material.PLAYER_HEAD) return this;
        return this;
    }

    @NotNull
    public ItemStack build() {
        return itemStack.clone();
    }

    @NotNull
    public ItemMeta getMeta() {
        return itemStack.getItemMeta();
    }
}
