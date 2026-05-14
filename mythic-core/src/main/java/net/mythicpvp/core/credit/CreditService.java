package net.mythicpvp.core.credit;

import net.mythicpvp.suite.api.Currency;
import net.mythicpvp.suite.economy.EconomyManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class CreditService {

    public long getBalance(@NotNull UUID player) {
        return EconomyManager.getInstance().getBalance(player, Currency.CREDITS);
    }

    public void give(@NotNull UUID player, long amount) {
        EconomyManager.getInstance().deposit(player, Currency.CREDITS, amount);
    }

    public boolean spend(@NotNull UUID player, long amount) {
        return EconomyManager.getInstance().withdraw(player, Currency.CREDITS, amount);
    }

    public void set(@NotNull UUID player, long amount) {
        EconomyManager.getInstance().setBalance(player, Currency.CREDITS, amount);
    }
}
