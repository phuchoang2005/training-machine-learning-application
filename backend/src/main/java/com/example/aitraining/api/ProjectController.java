package com.example.aitraining.api;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.dto.ApiDtos.*;
import com.example.aitraining.service.AuthorizationService;
import com.example.aitraining.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
    CreateProjectResponse uploadZip(@Valid @RequestPart("metadata") ZipProjectMetadata metadata) {
        return service.createZip(CurrentUserContext.require(), metadata);
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
