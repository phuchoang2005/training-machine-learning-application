package com.example.aitraining.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String storageRoot, Queue queue) {
    public record Queue(int runningLimit) {
    }
}
