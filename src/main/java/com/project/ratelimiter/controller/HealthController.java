package com.project.ratelimiter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoints for monitoring
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Health check and system status endpoints")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Basic health check
     */
    @GetMapping
    @Operation(summary = "Basic health check", description = "Returns UP if service is running")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "rate-limiter");
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Detailed health check including dependencies
     */
    @GetMapping("/detailed")
    @Operation(
            summary = "Detailed health check",
            description = "Returns status of all system components"
    )
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "UP");
        response.put("timestamp", LocalDateTime.now());

        Map<String, Object> dependencies = new HashMap<>();

        // Check PostgreSQL
        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(2);
            dependencies.put("postgres", isValid ? "UP" : "DOWN");

            if (isValid) {
                dependencies.put("postgres_metadata", Map.of(
                        "url", conn.getMetaData().getURL(),
                        "driver", conn.getMetaData().getDriverName()
                ));
            }
        } catch (Exception e) {
            logger.error("PostgreSQL health check failed", e);
            dependencies.put("postgres", "DOWN");
            dependencies.put("postgres_error", e.getMessage());
        }

        // Check Redis
        try {
            String testKey = "health:check:" + System.currentTimeMillis();
            String testValue = "OK";

            redisTemplate.opsForValue().set(testKey, testValue);
            String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);

            boolean redisOk = testValue.equals(retrievedValue);
            dependencies.put("redis", redisOk ? "UP" : "DOWN");

            if (redisOk) {
                dependencies.put("redis_metadata", Map.of(
                        "ping", "PONG"
                ));
            }
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            dependencies.put("redis", "DOWN");
            dependencies.put("redis_error", e.getMessage());
        }

        response.put("dependencies", dependencies);

        // Determine overall status
        boolean allUp = dependencies.values().stream()
                .filter(v -> v instanceof String)
                .allMatch(v -> "UP".equals(v));

        response.put("overall_status", allUp ? "UP" : "DEGRADED");

        return ResponseEntity.ok(response);
    }

    /**
     * Readiness probe for Kubernetes
     */
    @GetMapping("/ready")
    @Operation(summary = "Readiness check", description = "Checks if service is ready to accept traffic")
    public ResponseEntity<Map<String, String>> readiness() {
        // Add more sophisticated checks here if needed
        Map<String, String> response = new HashMap<>();
        response.put("status", "READY");

        return ResponseEntity.ok(response);
    }

    /**
     * Liveness probe for Kubernetes
     */
    @GetMapping("/live")
    @Operation(summary = "Liveness check", description = "Checks if service is alive")
    public ResponseEntity<Map<String, String>> liveness() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ALIVE");

        return ResponseEntity.ok(response);
    }
}