package com.interview.api.controller;

import com.interview.common.api.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    public HealthController(DataSource dataSource, StringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("service", "interview-api");
        details.put("timestamp", OffsetDateTime.now().toString());

        boolean dbUp = checkDatabase();
        boolean redisUp = checkRedis();

        details.put("database", dbUp ? "UP" : "DOWN");
        details.put("redis", redisUp ? "UP" : "DOWN");

        String overallStatus = (dbUp && redisUp) ? "UP" : "DEGRADED";
        details.put("status", overallStatus);

        return ApiResponse.ok(details);
    }

    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(3);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            return "PONG".equals(redisTemplate.getConnectionFactory().getConnection().ping());
        } catch (Exception e) {
            return false;
        }
    }
}
