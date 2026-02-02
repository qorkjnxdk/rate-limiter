package com.project.ratelimiter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class RateLimitRequest {

    @NotBlank(message = "Resource identifier is required")
    private String resource;

    private String userId;  // Optional - can be derived from API key
    private String metadata;  // Optional - additional context

}
