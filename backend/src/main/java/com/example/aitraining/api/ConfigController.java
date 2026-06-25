package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.dto.ProjectDtos.*;
import com.example.aitraining.service.AuthorizationService;
import com.example.aitraining.service.ConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Project configuration endpoints.
 *
 * <p>All operations require project ownership (owner only; admins are not permitted, consistent
 * with the sensitive-data policy).
 *
 * <p>Routes (all under {@code /api/v1}):
 * <ul>
 *   <li>{@code GET  /projects/{projectId}/configs} — list all configs for the project.</li>
 *   <li>{@code GET  /projects/{projectId}/configs/{configId}} — fetch a single config with its
 *       full YAML content and a content hash.</li>
 *   <li>{@code POST /projects/{projectId}/configs/validate} — parse and preview YAML without saving.</li>
 * </ul>
 */
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
