package com.project.ratelimiter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "rate_limit_configs",
       indexes = {
        @Index(name = "idx_user_resources", columnList = "user_id, resource", unique = true),
        @Index(name = "idx_tier", columnList = "tier")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(nullable = false, length = 255)
    private String resource;

    @Column(nullable = false, length = 50)
    private String tier; // "free", "premium", "enterprise"

    @Column(name = "requests_per_minute", nullable = false)
    private Integer requestsPerMinute;

    @Column(name = "burst_capacity")
    private Integer burstCapacity;

    @Column(length = 50)
    private String algorithm;

    @Column(nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    public boolean isActive() {
        return enabled != null && enabled;
    }
}
