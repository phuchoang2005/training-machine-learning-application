package com.example.aitraining.service;

import com.example.aitraining.auth.ForbiddenException;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.domain.Models.User;
import com.example.aitraining.repo.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthorizationService {
    private final ProjectRepository projects;

    public AuthorizationService(ProjectRepository projects) {
        this.projects = projects;
    }

    public Project requireProjectOwner(User user, UUID projectId) {
        Project project = projects.get(projectId);
        if (!project.ownerUserId().equals(user.userId())) {
            throw new ForbiddenException("Project ownership is required");
        }
        return project;
    }

    public Project requireProjectVisible(User user, UUID projectId) {
        Project project = projects.get(projectId);
        if (!user.isAdmin() && !project.ownerUserId().equals(user.userId())) {
            throw new ForbiddenException("Project is not visible to current user");
        }
        return project;
    }

    public Project requireJobVisible(User user, TrainingJob job) {
        Project project = projects.get(job.projectId());
        if (!user.isAdmin() && !project.ownerUserId().equals(user.userId())) {
            throw new ForbiddenException("Job is not visible to current user");
        }
        return project;
    }

    public Project requireSensitiveJobOwner(User user, TrainingJob job) {
        Project project = projects.get(job.projectId());
        if (!project.ownerUserId().equals(user.userId())) {
            throw new ForbiddenException("Ownership is required for logs and artifacts");
        }
        return project;
    }

    public void requireAdmin(User user) {
        if (!user.isAdmin()) {
            throw new ForbiddenException("Administrator role is required");
        }
    }
}
