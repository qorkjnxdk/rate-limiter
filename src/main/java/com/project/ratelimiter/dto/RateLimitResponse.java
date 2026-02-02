package com.project.ratelimiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder //Easier syntax for instantializing objects
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResponse {

    private boolean allowed;

    private long remainingTokens;

    private Instant resetTime;

    private String tier;

    private String message;

    private RateLimitMetadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitMetadata {
        private String algorithm;
        private long requestCount;
        private long windowDuration;
    }

}
