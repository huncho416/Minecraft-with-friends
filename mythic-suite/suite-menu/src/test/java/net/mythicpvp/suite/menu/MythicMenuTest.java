package net.mythicpvp.suite.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MythicMenuTest {

    @Test
    void paginatedMenuCalculatesPagesFromContentSlots() {
        PaginatedMenu menu = PaginatedMenu.create(6, "&#FF00F8Menu");
        assertEquals(1, menu.getMaxPages());
    }
}
