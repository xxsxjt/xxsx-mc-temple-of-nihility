package com.templenihility.action;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TickRateLimiterTest {
    @Test
    void rejectsRequestsInsideWindowAndAcceptsBoundary() {
        TickRateLimiter<String> limiter = new TickRateLimiter<>();

        assertTrue(limiter.tryAcquire("player", 100, 5));
        assertFalse(limiter.tryAcquire("player", 100, 5));
        assertFalse(limiter.tryAcquire("player", 104, 5));
        assertTrue(limiter.tryAcquire("player", 105, 5));
    }

    @Test
    void playersHaveIndependentWindows() {
        TickRateLimiter<String> limiter = new TickRateLimiter<>();

        assertTrue(limiter.tryAcquire("first", 10, 5));
        assertTrue(limiter.tryAcquire("second", 10, 5));
        assertFalse(limiter.tryAcquire("first", 11, 5));
    }

    @Test
    void clearAllowsImmediateReuse() {
        TickRateLimiter<String> limiter = new TickRateLimiter<>();
        limiter.tryAcquire("player", 20, 5);

        limiter.clear("player");

        assertTrue(limiter.tryAcquire("player", 20, 5));
    }

    @Test
    void clockResetDoesNotLockAPlayerIndefinitely() {
        TickRateLimiter<String> limiter = new TickRateLimiter<>();
        limiter.tryAcquire("player", 1_000, 5);

        assertTrue(limiter.tryAcquire("player", 10, 5));
    }
}
