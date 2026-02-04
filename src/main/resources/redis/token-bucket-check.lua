-- KEYS[1] = rate_limit:userId:resource:tokens
-- KEYS[2] = rate_limit:userId:resource:time
-- ARGV[1] = capacity (e.g., 10)
-- ARGV[2] = requestsPerMinute (e.g., 10)
-- ARGV[3] = current timestamp (milliseconds)
--
-- RETURNS:
-- [1] = allowed (1 = yes, 0 = no)
-- [2] = remaining tokens
-- [3] = reset time

-- Get current state from Redis
local tokens = redis.call('get', KEYS[1])
local lastRefillTime = redis.call('get', KEYS[2])
local now = tonumber(ARGV[3])
local capacity = tonumber(ARGV[1])
local requestsPerMinute = tonumber(ARGV[2])

-- Initialize if first request
if not tokens then
    tokens = capacity
    lastRefillTime = now
else
    tokens = tonumber(tokens)
    lastRefillTime = tonumber(lastRefillTime)
end

-- Calculate token refill
local elapsedMs = now - lastRefillTime
local refillRate = requestsPerMinute / 60000.0  -- tokens per millisecond
local tokensToAdd = elapsedMs * refillRate

-- Update tokens (cap at capacity)
tokens = math.min(capacity, tokens + tokensToAdd)

-- Check if request allowed
local allowed = 0
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

-- Save updated state: Auto-cleanup inactive users
redis.call('set', KEYS[1], tokens, 'EX', 120)  -- Expire in 120 seconds
redis.call('set', KEYS[2], now, 'EX', 120)

-- Calculate reset time
local tokensNeeded = capacity - tokens
local msUntilFull = tokensNeeded / refillRate
local resetTime = now + msUntilFull

-- Return results
return {allowed, math.floor(tokens), math.floor(resetTime)}