package com.project.ratelimiter.exception;

public class RateLimitExceededException extends RuntimeException{

    private final String userId;
    private final String resource;
    private final long resetTime;

    public RateLimitExceededException(String message) {
        super(message);
        this.userId = null;
        this.resource = null;
        this.resetTime = 0;
    }

    public RateLimitExceededException(String message, String userId, String resource, long resetTime) {
        super(message);
        this.userId = userId;
        this.resource = resource;
        this.resetTime = resetTime;
    }

    public String getUserId() {
        return userId;
    }

    public String getResource() {
        return resource;
    }

    public long getResetTime() {
        return resetTime;
    }
}
