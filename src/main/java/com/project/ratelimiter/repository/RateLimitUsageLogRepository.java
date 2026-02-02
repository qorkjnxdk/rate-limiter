package com.project.ratelimiter.repository;

import com.project.ratelimiter.model.RateLimitUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RateLimitUsageLogRepository extends JpaRepository<RateLimitUsageLog, Long>{

    //Find denied quests within a target time frame
    List<RateLimitUsageLog> findByAllowedFalseAndCreatedAtBetween(Instant start, Instant end);

    //Count requests by user in target time frame
    @Query("SELECT l.userId, COUNT(l) FROM RateLimitUsageLog l " +
            "WHERE l.createdAt BETWEEN :start AND :end " +
            "GROUP BY l.userId ORDER BY COUNT(l) DESC")
    List<Object[]> findTopUsersByRequestCount(@Param("start") Instant start,
                                              @Param("end") Instant end);

    //Calculate block rate (Percentage of total requests that are being denied)
    @Query("SELECT " +
            "COUNT(CASE WHEN l.allowed = false THEN 1 END) as denied, " +
            "COUNT(l) as total " +
            "FROM RateLimitUsageLog l " +
            "WHERE l.createdAt BETWEEN :start AND :end")
    Object[] calculateBlockRate(@Param("start") Instant start, @Param("end") Instant end);
}
