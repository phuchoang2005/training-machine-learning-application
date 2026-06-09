package com.example.aitraining.service;

import com.example.aitraining.domain.Models.ProjectConfig;
import com.example.aitraining.dto.ApiDtos.*;
import com.example.aitraining.repo.ConfigRepository;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConfigService {
    private final ConfigRepository configs;

    public ConfigService(ConfigRepository configs) {
        this.configs = configs;
    }

    public ProjectConfigListResponse list(UUID projectId) {
        return new ProjectConfigListResponse(configs.list(projectId).stream()
                .map(c -> new ProjectConfigSummary(c.configId(), c.configName(), c.configPath(), c.defaultConfig(), c.updatedAt()))
                .toList());
    }

    public ProjectConfigContent get(UUID projectId, UUID configId) {
        ProjectConfig c = configs.get(projectId, configId);
        return new ProjectConfigContent(c.configId(), c.configPath(), c.yamlContent(), configs.contentHash(c.yamlContent()));
    }

    public ValidateYamlResponse validate(String yamlContent) {
        try {
            Object loaded = new Yaml().load(yamlContent);
            Map<String, Object> preview = loaded instanceof Map<?, ?> map
                    ? map.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                    e -> String.valueOf(e.getKey()), Map.Entry::getValue))
                    : Map.of("value", loaded == null ? "" : loaded);
            return new ValidateYamlResponse(true, preview, List.of());
        } catch (RuntimeException ex) {
            return new ValidateYamlResponse(false, Map.of(), List.of(ex.getMessage()));
        }
    }
}
