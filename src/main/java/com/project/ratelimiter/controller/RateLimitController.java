package com.project.ratelimiter.controller;

import com.project.ratelimiter.dto.RateLimitRequest;
import com.project.ratelimiter.dto.RateLimitResponse;
import com.project.ratelimiter.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for rate limiting operations
 *
 * This follows REST best practices:
 * - POST for state-changing operations (checking limit changes token count)
 * - Proper HTTP status codes (200 OK, 429 Too Many Requests)
 * - Validation with @Valid
 * - Swagger documentation
 */

@RestController
@RequestMapping("/api")
@Tag(name = "Rate Limiting", description = "Rate limit checking and management")
public class RateLimitController {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitController.class);
    private final RateLimiterService rateLimiterService;

    public RateLimitController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    //Checking if a request should be allowed
    @PostMapping("/check-limit")
    @Operation(
            summary = "Check if request is allowed",
            description = "Validates if a request should be allowed based on rate limits. " +
                    "Returns 200 with allowed=true if OK, or allowed=false if rate limited."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Rate limit check completed",
                    content = @Content(schema = @Schema(implementation = RateLimitResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request format"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded (alternative response format)"
            )
    })
    public ResponseEntity<RateLimitResponse> checkLimit(
//            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Valid @RequestBody RateLimitRequest request) {

        // INTERVIEW TIP: In production, you'd validate the API key here
        // and map it to a userId. For this demo, we use userId from request.
        String userId = request.getUserId() != null ? request.getUserId() : "anonymous";

        logger.info("Rate limit check requested: userId={}, resource={}",
                userId, request.getResource());

        // Call the service
        RateLimitResponse response = rateLimiterService.allowRequest(userId, request.getResource());

        // Add metadata about algorithm used
        if (response.getMetadata() == null) {
            response.setMetadata(RateLimitResponse.RateLimitMetadata.builder()
                    .algorithm("TOKEN_BUCKET")
                    .build());
        }

        // Return appropriate status code -> 200 if allowed, 429 if denied
        HttpStatus status = response.isAllowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;

        return ResponseEntity.status(status).body(response);
    }

    //Check current rate limit status (Without consuming a token)
    @GetMapping("/limit-status")
    @Operation(
            summary = "Get rate limit status",
            description = "Check remaining quota without consuming a token"
    )
    public ResponseEntity<RateLimitResponse> getLimitStatus(
            @RequestParam String userId,
            @RequestParam String resource) {

        long remaining = rateLimiterService.getRemainingTokens(userId, resource);
        long resetTime = rateLimiterService.getResetTime(userId, resource);

        RateLimitResponse response = RateLimitResponse.builder()
                .allowed(remaining > 0)
                .remainingTokens(remaining)
                .resetTime(java.time.Instant.ofEpochMilli(resetTime))
                .message(remaining > 0 ? "Quota available" : "Quota exhausted")
                .build();

        return ResponseEntity.ok(response);
    }
}
