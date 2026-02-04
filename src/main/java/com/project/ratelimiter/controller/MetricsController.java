package com.project.ratelimiter.controller;

import com.project.ratelimiter.metrics.RateLimitMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "Rate limiter metrics and statistics")
public class MetricsController {

    private final RateLimitMetrics metrics;

    public MetricsController(RateLimitMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Get current metrics summary
     */
    @GetMapping("/summary")
    @Operation(summary = "Get metrics summary")
    public ResponseEntity<RateLimitMetrics.MetricsSummary> getMetricsSummary() {
        return ResponseEntity.ok(metrics.getSummary());
    }
}
