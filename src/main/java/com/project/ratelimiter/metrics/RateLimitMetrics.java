package com.project.ratelimiter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimitMetrics {

    private final Counter requestsAllowedCounter;
    private final Counter requestsDeniedCounter;
    private final Counter luaScriptSuccessCounter;
    private final Counter luaScriptFailureCounter;
    private final Timer rateLimitCheckTimer;

    public RateLimitMetrics(MeterRegistry meterRegistry) {
        // Counter: Total allowed requests
        this.requestsAllowedCounter = Counter.builder("rate_limiter.requests.allowed")
                .description("Total number of allowed requests")
                .tag("result", "allowed")
                .register(meterRegistry);

        // Counter: Total denied requests
        this.requestsDeniedCounter = Counter.builder("rate_limiter.requests.denied")
                .description("Total number of denied requests")
                .tag("result", "denied")
                .register(meterRegistry);

        // Counter: Lua script successes
        this.luaScriptSuccessCounter = Counter.builder("rate_limiter.lua.success")
                .description("Number of successful Lua script executions")
                .register(meterRegistry);

        // Counter: Lua script failures (fallback to Java)
        this.luaScriptFailureCounter = Counter.builder("rate_limiter.lua.failure")
                .description("Number of failed Lua script executions")
                .register(meterRegistry);

        // Timer: Rate limit check duration
        this.rateLimitCheckTimer = Timer.builder("rate_limiter.check.duration")
                .description("Time taken to check rate limit")
                .publishPercentiles(0.5, 0.95, 0.99) // p50, p95, p99
                .register(meterRegistry);
    }

    /**
     * Record an allowed request
     */
    public void recordAllowed() {
        requestsAllowedCounter.increment();
    }

    /**
     * Record a denied request
     */
    public void recordDenied() {
        requestsDeniedCounter.increment();
    }

    /**
     * Record Lua script success
     */
    public void recordLuaSuccess() {
        luaScriptSuccessCounter.increment();
    }

    /**
     * Record Lua script failure
     */
    public void recordLuaFailure() {
        luaScriptFailureCounter.increment();
    }

    /**
     * Time a rate limit check
     */
    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(rateLimitCheckTimer);
    }

    /**
     * Get metrics summary (for admin dashboard
     */
    public MetricsSummary getSummary() {
        long allowed = (long) requestsAllowedCounter.count();
        long denied = (long) requestsDeniedCounter.count();
        long total = allowed + denied;

        double blockRate = total > 0 ? (denied * 100.0 / total) : 0.0;

        return MetricsSummary.builder()
                .totalRequests(total)
                .allowedRequests(allowed)
                .deniedRequests(denied)
                .blockRatePercent(blockRate)
                .luaScriptSuccesses((long) luaScriptSuccessCounter.count())
                .luaScriptFailures((long) luaScriptFailureCounter.count())
                .build();
    }

    /**
     * DTO for metrics summary
     */
    @lombok.Data
    @lombok.Builder
    public static class MetricsSummary {
        private long totalRequests;
        private long allowedRequests;
        private long deniedRequests;
        private double blockRatePercent;
        private long luaScriptSuccesses;
        private long luaScriptFailures;
    }
}
