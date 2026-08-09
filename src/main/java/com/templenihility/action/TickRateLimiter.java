package com.templenihility.action;

import java.util.HashMap;
import java.util.Map;

public final class TickRateLimiter<K> {
    private final Map<K, Long> nextAllowedTicks = new HashMap<>();

    public boolean tryAcquire(K key, long now, long intervalTicks) {
        long interval = Math.max(0L, intervalTicks);
        Long nextAllowed = nextAllowedTicks.get(key);
        if (nextAllowed != null && now < nextAllowed && nextAllowed - now <= interval) {
            return false;
        }
        long next = now > Long.MAX_VALUE - interval ? Long.MAX_VALUE : now + interval;
        nextAllowedTicks.put(key, next);
        return true;
    }

    public void clear(K key) {
        nextAllowedTicks.remove(key);
    }
}
