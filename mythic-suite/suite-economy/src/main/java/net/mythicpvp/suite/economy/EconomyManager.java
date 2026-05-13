package net.mythicpvp.suite.economy;

import net.mythicpvp.suite.api.Currency;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EconomyManager {

    private static final EconomyManager INSTANCE = new EconomyManager();
    private final Map<String, Long> balances = new ConcurrentHashMap<>();

    private EconomyManager() {}

    @NotNull
    public static EconomyManager getInstance() {
        return INSTANCE;
    }

    public long getBalance(@NotNull UUID player, @NotNull Currency currency) {
        return balances.getOrDefault(key(player, currency), 0L);
    }

    public void setBalance(@NotNull UUID player, @NotNull Currency currency, long amount) {
        balances.put(key(player, currency), Math.max(0, amount));
    }

    public void deposit(@NotNull UUID player, @NotNull Currency currency, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balances.compute(key(player, currency), (ignored, current) -> Math.addExact(current == null ? 0L : current, amount));
    }

    public boolean withdraw(@NotNull UUID player, @NotNull Currency currency, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be positive");
        }
        String key = key(player, currency);
        long[] result = new long[1];
        balances.compute(key, (ignored, current) -> {
            long balance = current == null ? 0L : current;
            if (balance < amount) {
                result[0] = 0;
                return balance;
            }
            result[0] = 1;
            return balance - amount;
        });
        return result[0] == 1;
    }

    public boolean has(@NotNull UUID player, @NotNull Currency currency, long amount) {
        return getBalance(player, currency) >= amount;
    }

    public void reset(@NotNull UUID player) {
        for (Currency c : Currency.values()) {
            balances.remove(key(player, c));
        }
    }

    @NotNull
    private String key(@NotNull UUID player, @NotNull Currency currency) {
        return player.toString() + ":" + currency.name();
    }
}
