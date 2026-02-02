package com.project.ratelimiter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "rate_limit_usage_logs",
        indexes = {
                @Index(name = "idx_user_timestamp", columnList = "user_id,created_at"),
                @Index(name = "idx_allowed", columnList = "allowed,created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(nullable = false, length = 255)
    private String resource;

    @Column(nullable = false)
    private Boolean allowed;

    @Column(name = "remaining_tokens")
    private Long remainingTokens;

    @Column(length = 50)
    private String algorithm;

    //Keeps record of the response time for rate limit check
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    //Tracks IP Address of request to prevent abuse
    @Column(name = "ip_address", length = 45) // IPv6 max length
    private String ipAddress;

    //User Agent
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
