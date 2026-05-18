package com.fooddelivery.apigateway.ratelimit;

/**
 * Thread-safe token bucket.
 * Starts full; refills continuously at (requestsPerMinute / 60 000) tokens per millisecond.
 */
public class TokenBucket {

    private final double capacity;
    private final double refillRatePerMs;   // tokens added per millisecond
    private double tokens;
    private long lastRefillTime;

    public TokenBucket(int requestsPerMinute) {
        this.capacity         = requestsPerMinute;
        this.refillRatePerMs  = requestsPerMinute / 60_000.0;
        this.tokens           = requestsPerMinute;   // start full
        this.lastRefillTime   = System.currentTimeMillis();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now     = System.currentTimeMillis();
        double added = (now - lastRefillTime) * refillRatePerMs;
        tokens           = Math.min(capacity, tokens + added);
        lastRefillTime   = now;
    }
}