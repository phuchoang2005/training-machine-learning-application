package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.dto.ApiDtos.*;
import com.example.aitraining.service.AuthorizationService;
import com.example.aitraining.service.ConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ConfigController {
    private final AuthorizationService authorization;
    private final ConfigService configs;

    public ConfigController(AuthorizationService authorization, ConfigService configs) {
        this.authorization = authorization;
        this.configs = configs;
    }

    @GetMapping("/projects/{projectId}/configs")
    ProjectConfigListResponse list(@PathVariable UUID projectId) {
        authorization.requireProjectOwner(CurrentUserContext.require(), projectId);
        return configs.list(projectId);
    }

    @GetMapping("/projects/{projectId}/configs/{configId}")
    ProjectConfigContent get(@PathVariable UUID projectId, @PathVariable UUID configId) {
        authorization.requireProjectOwner(CurrentUserContext.require(), projectId);
        return configs.get(projectId, configId);
    }

    @PostMapping("/projects/{projectId}/configs/validate")
    ValidateYamlResponse validate(@PathVariable UUID projectId, @Valid @RequestBody ValidateYamlRequest request) {
        authorization.requireProjectOwner(CurrentUserContext.require(), projectId);
        return configs.validate(request.yamlContent());
    }
}
