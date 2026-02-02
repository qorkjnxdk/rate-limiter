package com.project.ratelimiter.repository;

import com.project.ratelimiter.model.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {

    // Finds config by specific user and resource
    Optional<RateLimitConfig> findByUserIdAndResourceAndEnabledTrue(String userId, String resource);

    //Find all enabled configs for a user
    List<RateLimitConfig> findByUserIdAndEnabledTrue(String userId);

    //Find all configs for a specific tier
    List<RateLimitConfig> findByTierAndEnabledTrue(String tier);

    //Checks if a config exists for a particular user + resource combination
    boolean existsByUserIdAndResourceAndEnabledTrue(String userId, String resource);

    //Finds all configs for a user (Disabled + Enabled)
    @Query("SELECT c FROM RateLimitConfig c WHERE c.userId = :userId AND c.enabled = true ORDER BY c.createdAt DESC")
    List<RateLimitConfig> findActiveConfigsByUserOrdered(@Param("userId") String userId);

    //Count active configs by tier
    @Query("SELECT c.tier, COUNT(c) FROM RateLimitConfig c WHERE c.enabled = true GROUP BY c.tier")
    List<Object[]> countByTier();

    //Find configs that have been disabled for >30 days
    @Query("SELECT c FROM RateLimitConfig c WHERE c.enabled = false AND c.updatedAt < :cutoffDate")
    List<RateLimitConfig> findDisabledConfigsOlderThan(@Param("cutoffDate") java.time.Instant cutoffDate);
}
