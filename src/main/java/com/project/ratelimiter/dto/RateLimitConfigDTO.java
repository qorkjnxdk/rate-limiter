package com.project.ratelimiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfigDTO {

    private Long id;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Resource is required")
    private String resource;

    @NotBlank(message = "Tier is required")
    private String tier;

    @NotNull(message = "Requests per minute is required")
    @Min(value = 1, message = "Requests per minute must be at least 1")
    private Integer requestsPerMinute;

    @Min(value = 1, message = "Burst capacity must be at least 1")
    private Integer burstCapacity;

    private String algorithm;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

}
