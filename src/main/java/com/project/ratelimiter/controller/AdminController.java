package com.project.ratelimiter.controller;

import com.project.ratelimiter.dto.RateLimitConfigDTO;
import com.project.ratelimiter.model.RateLimitConfig;
import com.project.ratelimiter.repository.RateLimitConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API Endpoint for Admin Operations
 */
@RestController
@RequestMapping("/api/admin/limits")
@Tag(name = "Admin - Rate Limit Management", description = "CRUD operations for rate limit configurations")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final RateLimitConfigRepository repository;

    public AdminController(RateLimitConfigRepository repository) {
        this.repository = repository;
    }

    //List of all rate limit configs
    @GetMapping
    @Operation(summary = "List all rate limit configurations")
    public ResponseEntity<List<RateLimitConfigDTO>> getAllConfigs() {
        logger.info("Fetching all rate limit configurations");

        List<RateLimitConfig> configs = repository.findAll();

        // Convert entities to DTOs
        // INTERVIEW TIP: Never expose entities directly
        // - Entities have JPA annotations, bidirectional relations, etc.
        // - DTOs are clean API contracts
        List<RateLimitConfigDTO> dtos = configs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    //Get a specific configuration by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get rate limit config by ID")
    public ResponseEntity<RateLimitConfigDTO> getConfigById(@PathVariable Long id){
        logger.info("Fetching config with id={}", id);

        RateLimitConfig config = repository.findById(id)
                .orElseThrow(()-> new RuntimeException("Config not found with id: "+id));

        return ResponseEntity.ok(toDto(config));
    }

    //Get config for specific user
    @GetMapping("/user/{userId}")
    @Operation(summary="Get all configs for a user")
    public ResponseEntity<List<RateLimitConfigDTO>> getConfigsByUser(@PathVariable String userId){
        logger.info("Fetching configs for UserId={}", userId);

        List<RateLimitConfig> configs = repository.findByUserIdAndEnabledTrue(userId);
        List<RateLimitConfigDTO> dtos = configs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    //Create new rate limit config
    @PostMapping
    @Operation(summary = "Create new rate limit configuration")
    public ResponseEntity<RateLimitConfigDTO> createConfig(@Valid @RequestBody RateLimitConfigDTO dto){
        logger.info("Creating config for userId={}, resource = {}", dto.getUserId(), dto.getResource());

        if (repository.existsByUserIdAndResourceAndEnabledTrue(dto.getUserId(),dto.getResource())){
            throw new RuntimeException("Config already exists for user " + dto.getUserId() +
                " and resource " + dto.getResource());
        }

        RateLimitConfig config = toEntity(dto);

        //Using default save method for repository
        RateLimitConfig saved = repository.save(config);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toDto(saved));
    }

    //Update an existing config
    @PutMapping("/{id}")
    @Operation(summary = "Update rate limit configuration")
    public ResponseEntity<RateLimitConfigDTO> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody RateLimitConfigDTO dto){
        logger.info("Updating config id={}", id);

        RateLimitConfig existing = repository.findById(id)
                .orElseThrow(()->new RuntimeException("Config not found with id: "+ id));

        //Updating Fields
        existing.setUserId(dto.getUserId());
        existing.setResource(dto.getResource());
        existing.setTier(dto.getTier());
        existing.setRequestsPerMinute(dto.getRequestsPerMinute());
        existing.setBurstCapacity(dto.getBurstCapacity());
        existing.setAlgorithm(dto.getAlgorithm());
        existing.setEnabled(dto.getEnabled());

        //Using default save method to update fields
        RateLimitConfig updated = repository.save(existing);

        logger.info("Updated config id={}", id);

        return ResponseEntity.ok(toDto(updated));
    }

    //"Soft-Deleting" existing config
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete rate limit config")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id){
        logger.info("Deleting config id={}", id);

        RateLimitConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found with id: " + id));

        // Soft delete - just disable it
        config.setEnabled(false);
        repository.save(config);

        logger.info("Disabled config id={}", id);

        //Return HTTP Status Code 204
        return ResponseEntity.noContent().build();
    }

    //Converting RateLimitConfig to RateLimitConfigDTO
    private RateLimitConfigDTO toDto (RateLimitConfig config){
        return RateLimitConfigDTO.builder()
                .id(config.getId())
                .userId(config.getUserId())
                .resource(config.getResource())
                .tier(config.getTier())
                .requestsPerMinute(config.getRequestsPerMinute())
                .burstCapacity(config.getBurstCapacity())
                .algorithm(config.getAlgorithm())
                .enabled(config.getEnabled())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    //Converting RateLimitConfigDTO to RateLimitConfig
    private RateLimitConfig toEntity(RateLimitConfigDTO dto) {
        return RateLimitConfig.builder()
                .userId(dto.getUserId())
                .resource(dto.getResource())
                .tier(dto.getTier())
                .requestsPerMinute(dto.getRequestsPerMinute())
                .burstCapacity(dto.getBurstCapacity())
                .algorithm(dto.getAlgorithm())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .build();
    }
}
