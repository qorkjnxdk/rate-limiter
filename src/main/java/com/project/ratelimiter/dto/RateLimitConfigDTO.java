package com.project.ratelimiter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Min(value = 1, message = "Requests per minute must be at least 1")
    private Integer requestsPerMinute;

    private Integer burstCapacity;
    private String algorithm;
    private Boolean enabled;

}
