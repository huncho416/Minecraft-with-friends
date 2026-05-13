package net.mythicpvp.suite.economy;

import net.mythicpvp.suite.api.Currency;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyManagerTest {

    @Test
    void managesBalancesAtomically() {
        EconomyManager manager = EconomyManager.getInstance();
        UUID player = UUID.randomUUID();
        manager.reset(player);
        manager.deposit(player, Currency.COINS, 100);
        assertTrue(manager.withdraw(player, Currency.COINS, 40));
        assertFalse(manager.withdraw(player, Currency.COINS, 100));
        assertEquals(60, manager.getBalance(player, Currency.COINS));
        assertThrows(IllegalArgumentException.class, () -> manager.deposit(player, Currency.COINS, 0));
    }
}
