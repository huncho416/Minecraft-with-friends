package net.mythicpvp.core.staff;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface StaffAudience {
    void accept(@NotNull StaffMessage message);
}
