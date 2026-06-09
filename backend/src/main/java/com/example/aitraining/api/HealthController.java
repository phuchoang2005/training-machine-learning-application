package com.example.aitraining.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/health")
    Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "future-api",
                "instance", System.getenv().getOrDefault("HOSTNAME", "unknown"),
                "timestamp", Instant.now().toString());
    }
}
