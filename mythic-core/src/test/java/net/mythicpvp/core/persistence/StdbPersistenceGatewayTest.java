package net.mythicpvp.core.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-fn tests for the conversion helpers inside
 * {@link StdbPersistenceGateway}. The full gateway exercise lives in
 * {@link PersistenceWiringTest}; this file pins the duration-mapping
 * contract that bridges mythic-core's `expiresAtMillis` (absolute epoch
 * milliseconds, 0 = permanent) with STDB's `duration_seconds` (relative
 * to now, 0 = permanent).
 */
class StdbPersistenceGatewayTest {

    @Test
    void zeroExpiryMapsToZeroDuration() {
        assertEquals(0L, StdbPersistenceGateway.durationSecondsFromExpiry(1000L, 0L));
        assertEquals(0L, StdbPersistenceGateway.durationSecondsFromExpiry(1000L, -5L));
    }

    @Test
    void positiveExpiryMapsToSecondsRelativeToCreation() {
        long createdMillis = 1_000_000L;
        long expiresMillis = createdMillis + 60_000L; // 60 seconds out
        assertEquals(60L, StdbPersistenceGateway.durationSecondsFromExpiry(createdMillis, expiresMillis));
    }

    @Test
    void subSecondExpiryClampsToOneSecond() {
        // 500ms duration would round to 0 seconds and STDB would treat
        // that as permanent. The helper clamps to a minimum of 1s so a
        // technically-temporary punishment doesn't accidentally become
        // permanent on the persistence side.
        long createdMillis = 1_000L;
        long expiresMillis = 1_500L;
        assertEquals(1L, StdbPersistenceGateway.durationSecondsFromExpiry(createdMillis, expiresMillis));
    }

    @Test
    void expiryBeforeCreationClampsToZero() {
        // Defensive: a clock skew or bad input shouldn't underflow into
        // a negative number that STDB would reject.
        assertEquals(0L, StdbPersistenceGateway.durationSecondsFromExpiry(2000L, 1000L));
    }
}
