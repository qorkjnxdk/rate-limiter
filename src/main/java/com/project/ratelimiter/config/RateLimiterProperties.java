package com.project.ratelimiter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for rate limiter
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private Default defaultConfig = new Default();
    private String algorithm = "TOKEN_BUCKET";
    private Map<String, TierConfig> tiers = new HashMap<>();

    @Data
    public static class Default {
        private int requestsPerMinute = 10;
        private int windowSeconds = 60;
    }

    @Data
    public static class TierConfig {
        private int requestsPerMinute;
        private int burstCapacity;
    }
}