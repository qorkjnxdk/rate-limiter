package com.project.ratelimiter.service.impl;

import com.project.ratelimiter.config.RateLimiterProperties;
import com.project.ratelimiter.dto.RateLimitResponse;
import com.project.ratelimiter.exception.RateLimitExceededException;
import com.project.ratelimiter.service.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Token Bucket Rate Limiter Implementation
 */

@Service
public class TokenBucketRateLimiter implements RateLimiterService{

    private static final Logger logger = LoggerFactory.getLogger(TokenBucketRateLimiter.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimiterProperties properties;

    public TokenBucketRateLimiter(RedisTemplate<String, Object> redisTemplate,
                                  RateLimiterProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    private static class TokenBucketState {
        double tokens;
        long lastRefillTime;
    }

    //Helper Function to Generate Key from userId + resource
    private String generateKey(String userId, String resource) {
        return String.format("rate_limit:%s:%s", userId, resource);
    }

    @Override
    public RateLimitResponse allowRequest(String userId, String resource){

        //Getting Redis state and checking if request is allowed
        String key = generateKey(userId,resource);
        TokenBucketState state = getCurrentState(key);
        refillTokens(state);
        boolean allowed = state.tokens>=1;

        if (allowed){
            state.tokens-=1;
            saveState(key,state);
            logger.debug("Request allowed for user={}, resource={}, remaining={}",
                    userId, resource, state.tokens);

            return buildResponse(true, state);
        } else{
            logger.warn("Rate limit exceeded for user={}, resource={}", userId, resource);
            return buildResponse(false, state);
        }
    }

    /**
     Token Refill Logic
     */

    private void refillTokens(TokenBucketState state){
        long now = System.currentTimeMillis();
        long capacity = properties.getDefaultConfig().getRequestsPerMinute();

        if (state.lastRefillTime == 0){
            state.tokens = capacity;
            state.lastRefillTime = now;
            return;
        }

        //Calculated time elapsed since last token refill
        long elapsedMs = now - state.lastRefillTime;

        //Calculate refill rate: tokens per millisecond
        double refillRate = (double) capacity / 60000.0;

        //Calculate tokens to be added:
        double tokensToAdd = elapsedMs*refillRate;

        //Update both fields in state
        state.tokens = Math.min(capacity, state.tokens + tokensToAdd);
        state.lastRefillTime = now;

        logger.debug("Refilled tokens: elapsed={}ms, tokensAdded={}, currentTokens={}",
                elapsedMs, tokensToAdd, state.tokens);
    }

    private TokenBucketState getCurrentState(String key){
        TokenBucketState state = new TokenBucketState();

        //Getting data from Redis
        Object tokensObj = redisTemplate.opsForHash().get(key, "tokens");
        Object timeObj = redisTemplate.opsForHash().get(key, "lastRefillTime");

        //For first requests, start with full bucket
        if (tokensObj != null){
            state.tokens = Double.parseDouble(tokensObj.toString());
        }
        else{
            state.tokens = properties.getDefaultConfig().getRequestsPerMinute();
        }

        if (timeObj != null){
            state.lastRefillTime = Long.parseLong(timeObj.toString());
        }
        else{
            state.lastRefillTime = 0;
        }

        return state;
    }

    private void saveState(String key, TokenBucketState state){
        redisTemplate.opsForHash().put(key, "tokens", state.tokens);
        redisTemplate.opsForHash().put(key, "lastRefillTime", state.lastRefillTime);
        redisTemplate.expire(key, 2, TimeUnit.MINUTES);
    }

    /**
     * Build response DTO
     */
    private RateLimitResponse buildResponse(boolean allowed, TokenBucketState state){
        long capacity = properties.getDefaultConfig().getRequestsPerMinute();

        //Calculates when bucket will be full again
        long tokensneeded = capacity - (long) Math.floor(state.tokens);
        double refillRate = (double) capacity / 60000.0;
        long msUntilFull = (long) (tokensneeded / refillRate);

        return RateLimitResponse.builder()
                .allowed(allowed)
                .remainingTokens((long) Math.floor(state.tokens))
                .resetTime(Instant.ofEpochMilli(state.lastRefillTime+msUntilFull))
                .message(allowed?"Request allowed" : "Rate limit exceeded. Try again later.")
                .build();
    }

    @Override
    public long getRemainingTokens(String userId, String resource){
        String key = generateKey(userId,resource);
        TokenBucketState state = getCurrentState(key);

        refillTokens(state);
        return (long) Math.floor(state.tokens);
    }

    @Override
    public long getResetTime(String userId, String resource) {
        String key = generateKey(userId, resource);
        TokenBucketState state = getCurrentState(key);
        refillTokens(state);

        long capacity = properties.getDefaultConfig().getRequestsPerMinute();
        long tokensNeeded = capacity - (long) Math.floor(state.tokens);
        double refillRate = (double) capacity / 60000.0;
        long msUntilFull = (long) (tokensNeeded / refillRate);

        return state.lastRefillTime + msUntilFull;
    }
}
