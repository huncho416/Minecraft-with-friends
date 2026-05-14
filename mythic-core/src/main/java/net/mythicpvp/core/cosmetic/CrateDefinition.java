package net.mythicpvp.core.cosmetic;

import net.mythicpvp.suite.api.Currency;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record CrateDefinition(
        @NotNull String id,
        @NotNull String displayName,
        long cost,
        @NotNull Currency currency,
        @NotNull List<CrateEntry> entries) {

    public record CrateEntry(@NotNull String cosmeticId, int weight) {}

    public int totalWeight() {
        return entries.stream().mapToInt(CrateEntry::weight).sum();
    }
}
