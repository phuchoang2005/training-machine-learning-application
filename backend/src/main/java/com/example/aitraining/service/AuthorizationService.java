package com.example.aitraining.service;

import com.example.aitraining.auth.ForbiddenException;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.TrainingJob;
import com.example.aitraining.domain.Models.User;
import com.example.aitraining.repo.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * <b>Facade Pattern</b> — provides a single, authoritative surface for all access-control
 * decisions in the application (NFR-SEC-004, NFR-SEC-005).
 *
 * <p>Controllers and services call one of the {@code require*()} methods instead of
 * implementing RBAC rules themselves.  Each method loads the target resource <em>and</em>
 * enforces the policy in one step, so callers never need to retrieve the project separately.
 *
 * <h2>Role model (development-grade)</h2>
 * <ul>
 *   <li>{@code USER} — can access only their own projects and jobs.</li>
 *   <li>{@code ADMIN} — can view any project/job but cannot read sensitive data
 *       (logs, artifacts) unless they also own the project.</li>
 * </ul>
 */
@Service
public class AuthorizationService {
  private final ProjectRepository projects;

  /**
   * @param projects used to load the project when resolving ownership checks
   */
  public AuthorizationService(ProjectRepository projects) {
    this.projects = projects;
  }

  /**
   * Asserts that {@code user} is the owner of the project.
   * Admins who do not own the project are also rejected.
   *
   * @param user      the caller from {@link com.example.aitraining.auth.CurrentUserContext}
   * @param projectId the project to check
   * @return the resolved project
   * @throws com.example.aitraining.auth.ForbiddenException if the user is not the owner
   * @throws org.springframework.dao.EmptyResultDataAccessException if the project is not found
   */
  public Project requireProjectOwner(User user, UUID projectId) {
    Project project = projects.get(projectId);
    if (!project.ownerUserId().equals(user.userId())) {
      throw new ForbiddenException("Project ownership is required");
    }
    return project;
  }

  /**
   * Asserts that the project is visible to {@code user}.
   * Admins can see all projects; regular users can see only their own.
   *
   * @return the resolved project
   * @throws com.example.aitraining.auth.ForbiddenException if visibility is denied
   */
  public Project requireProjectVisible(User user, UUID projectId) {
    Project project = projects.get(projectId);
    if (!user.isAdmin() && !project.ownerUserId().equals(user.userId())) {
      throw new ForbiddenException("Project is not visible to current user");
    }
    return project;
  }

  /**
   * Asserts that the job's parent project is visible to {@code user}.
   *
   * @param job a previously loaded job whose {@code projectId} is looked up
   * @return the job's parent project
   * @throws com.example.aitraining.auth.ForbiddenException if visibility is denied
   */
  public Project requireJobVisible(User user, TrainingJob job) {
    Project project = projects.get(job.projectId());
    if (!user.isAdmin() && !project.ownerUserId().equals(user.userId())) {
      throw new ForbiddenException("Job is not visible to current user");
    }
    return project;
  }

  /**
   * Asserts that {@code user} is the project owner for a sensitive data access (logs,
   * artifacts).  Unlike {@link #requireProjectVisible}, admins are also rejected here
   * (NFR-SEC-005).
   *
   * @return the job's parent project
   * @throws com.example.aitraining.auth.ForbiddenException if the user is not the owner
   */
  public Project requireSensitiveJobOwner(User user, TrainingJob job) {
    Project project = projects.get(job.projectId());
    if (!project.ownerUserId().equals(user.userId())) {
      throw new ForbiddenException("Ownership is required for logs and artifacts");
    }
    return project;
  }

  /**
   * Asserts that {@code user} holds the {@code ADMIN} role.
   *
   * @throws com.example.aitraining.auth.ForbiddenException if the user is not an admin
   */
  public void requireAdmin(User user) {
    if (!user.isAdmin()) {
      throw new ForbiddenException("Administrator role is required");
    }
  }
}
