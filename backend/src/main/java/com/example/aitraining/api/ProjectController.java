package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.dto.ProjectDtos.*;
import com.example.aitraining.service.AuthorizationService;
import com.example.aitraining.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Project management endpoints.
 *
 * <p>Routes (all under {@code /api/v1}):
 * <ul>
 *   <li>{@code GET  /projects} — list visible projects (admin sees all, user sees own).
 *       Optional {@code ?query=} substring filter on project name.</li>
 *   <li>{@code POST /projects} — create a GitHub-source project.</li>
 *   <li>{@code POST /projects/upload-zip} — create a ZIP-source project via multipart upload
 *       ({@code metadata} part: JSON, {@code file} part: ZIP archive).</li>
 *   <li>{@code GET  /projects/{projectId}} — get project detail; visible to owner and admin.</li>
 *   <li>{@code DELETE /projects/{projectId}} — delete project and all associated data;
 *       admin may delete any, owner only for own.</li>
 * </ul>
 */
@RestController
public class ProjectController {
  private final ProjectService service;
  private final AuthorizationService authorization;

  public ProjectController(ProjectService service, AuthorizationService authorization) {
    this.service = service;
    this.authorization = authorization;
  }

  @GetMapping("/projects")
  ProjectPage list(@RequestParam(required = false) String query,
      @RequestParam(defaultValue = "50") int limit) {
    return service.list(CurrentUserContext.require(), query, limit);
  }

  @PostMapping("/projects")
  @ResponseStatus(HttpStatus.CREATED)
  CreateProjectResponse createGithub(@Valid @RequestBody CreateGithubProjectRequest request) {
    return service.createGithub(CurrentUserContext.require(), request);
  }

  @PostMapping("/projects/upload-zip")
  @ResponseStatus(HttpStatus.CREATED)
  CreateProjectResponse uploadZip(
      @Valid @RequestPart("metadata") ZipProjectMetadata metadata,
      @RequestPart("file") MultipartFile file) {
    return service.createZip(CurrentUserContext.require(), metadata, file);
  }

  @GetMapping("/projects/{projectId}")
  ProjectDetail get(@PathVariable UUID projectId) {
    var user = CurrentUserContext.require();
    return service.detail(authorization.requireProjectVisible(user, projectId));
  }

  @DeleteMapping("/projects/{projectId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID projectId) {
    var user = CurrentUserContext.require();
    var project = user.isAdmin()
        ? authorization.requireProjectVisible(user, projectId)
        : authorization.requireProjectOwner(user, projectId);
    service.delete(user, project);
  }
}
