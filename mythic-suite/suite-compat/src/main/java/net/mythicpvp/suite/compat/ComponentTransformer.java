package net.mythicpvp.suite.compat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ComponentTransformer {

    private static final int CACHE_SIZE_PER_TIER = 5000;

    private final Map<ProfileTier, LruCache> caches;

    public ComponentTransformer() {
        this.caches = new LinkedHashMap<>();
        for (ProfileTier tier : ProfileTier.values()) {
            caches.put(tier, new LruCache(CACHE_SIZE_PER_TIER));
        }
    }

    @NotNull
    public Component transform(@NotNull Component input, @NotNull ClientProfile profile) {
        return transformForTier(input, profile.tier());
    }

    @NotNull
    public Component transformForTier(@NotNull Component input, @NotNull ProfileTier tier) {
        if (tier.rendersFully()) return input;
        LruCache cache = caches.get(tier);
        synchronized (cache) {
            Component cached = cache.get(input);
            if (cached != null) return cached;
        }
        Component transformed = walk(input, tier);
        synchronized (cache) {
            cache.put(input, transformed);
        }
        return transformed;
    }

    @NotNull
    public List<Component> transform(@NotNull List<Component> inputs, @NotNull ClientProfile profile) {
        if (profile.tier().rendersFully()) return inputs;
        List<Component> out = new ArrayList<>(inputs.size());
        for (Component input : inputs) {
            out.add(transformForTier(input, profile.tier()));
        }
        return out;
    }

    public void clearCaches() {
        for (LruCache cache : caches.values()) {
            synchronized (cache) {
                cache.clear();
            }
        }
    }

    @NotNull
    public Map<ProfileTier, Integer> cacheSizes() {
        Map<ProfileTier, Integer> sizes = new LinkedHashMap<>();
        for (Map.Entry<ProfileTier, LruCache> entry : caches.entrySet()) {
            synchronized (entry.getValue()) {
                sizes.put(entry.getKey(), entry.getValue().size());
            }
        }
        return sizes;
    }

    @NotNull
    private Component walk(@NotNull Component component, @NotNull ProfileTier tier) {
        Style style = component.style();
        Style newStyle = downgradeStyle(style, tier);
        Component result = component.style(newStyle);
        List<Component> children = result.children();
        if (!children.isEmpty()) {
            List<Component> newChildren = new ArrayList<>(children.size());
            for (Component child : children) {
                newChildren.add(walk(child, tier));
            }
            result = result.children(newChildren);
        }
        return result;
    }

    @NotNull
    private Style downgradeStyle(@NotNull Style style, @NotNull ProfileTier tier) {
        Style.Builder builder = style.toBuilder();
        TextColor color = style.color();
        if (color != null && tier.needsHexDowngrade() && !(color instanceof NamedTextColor)) {
            builder.color(LegacyColorMapper.nearestLegacy(color));
        }
        if (tier == ProfileTier.BEDROCK) {
            builder.decoration(TextDecoration.OBFUSCATED, TextDecoration.State.NOT_SET);
        }
        return builder.build();
    }

    private static final class LruCache extends LinkedHashMap<Component, Component> {
        private final int max;

        LruCache(int max) {
            super(64, 0.75f, true);
            this.max = max;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Component, Component> eldest) {
            return size() > max;
        }

        @NotNull
        Map<Component, Component> snapshot() {
            return Collections.unmodifiableMap(this);
        }
    }
}
