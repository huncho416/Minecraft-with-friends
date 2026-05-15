package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.api.Currency;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record CrateDefinition(
        @NotNull String id,
        @NotNull String displayName,
        long cost,
        @NotNull Currency currency,
        @NotNull List<CrateEntry> entries,
        long availableFrom,
        long availableUntil) {

    public record CrateEntry(@NotNull String cosmeticId, int weight) {}

    public int totalWeight() {
        return entries.stream().mapToInt(CrateEntry::weight).sum();
    }

    public boolean isAvailable() {
        long now = System.currentTimeMillis();
        if (availableFrom > 0 && now < availableFrom) return false;
        if (availableUntil > 0 && now > availableUntil) return false;
        return true;
    }
}
