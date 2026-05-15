package net.mythicpvp.core.credit;

import net.mythicpvp.suite.api.Currency;
import net.mythicpvp.suite.economy.EconomyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreditServiceTest {

    private CreditService service;
    private UUID player;

    @BeforeEach
    void setUp() {
        player = UUID.randomUUID();
        service = new CreditService();
    }

    @Test
    void defaultBalanceIsZero() {
        assertEquals(0, service.getBalance(player));
    }

    @Test
    void giveIncreasesBalance() {
        service.give(player, 500);
        assertEquals(500, service.getBalance(player));
    }

    @Test
    void giveAccumulates() {
        service.give(player, 300);
        service.give(player, 200);
        assertEquals(500, service.getBalance(player));
    }

    @Test
    void spendDeductsBalance() {
        service.give(player, 1000);
        boolean success = service.spend(player, 400);
        assertTrue(success);
        assertEquals(600, service.getBalance(player));
    }

    @Test
    void spendFailsOnInsufficientBalance() {
        service.give(player, 100);
        boolean success = service.spend(player, 200);
        assertFalse(success);
        assertEquals(100, service.getBalance(player));
    }

    @Test
    void setOverridesBalance() {
        service.give(player, 500);
        service.set(player, 100);
        assertEquals(100, service.getBalance(player));
    }

    @Test
    void balanceUsesCreditsEnumValue() {
        service.give(player, 750);
        assertEquals(750, EconomyManager.getInstance().getBalance(player, Currency.CREDITS));
    }
}
