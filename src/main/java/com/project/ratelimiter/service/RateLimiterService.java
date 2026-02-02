package com.project.ratelimiter.service;

import com.project.ratelimiter.dto.RateLimitResponse;

/**
    * Service interface for rate limiting operations.
    * Allows us to have multiple implementations (Token Bucket, Sliding Window, Leaky Bucket) without changing client code.
 */

public interface RateLimiterService {

    /**
     * Check if a request should be allowed based on rate limits.
     *
     * @param userId The user making the request
     * @param resource The resource being accessed (e.g., "api/users")
     * @return RateLimitResponse with allowed status and metadata

     */
    RateLimitResponse allowRequest(String userId, String resource);

    /**
     * Get remaining tokens for a user.
     * Useful for showing users their quota in API responses.
     */
    long getRemainingTokens(String userId, String resource);

    /**
     * Get when the rate limit window resets.
     * Important for "Retry-After" HTTP headers.
     */
    long getResetTime(String userId, String resource);

}
