package com.phuchoang2005.aitraining.service;

import com.phuchoang2005.aitraining.auth.ForbiddenException;
import com.phuchoang2005.aitraining.domain.Enums.UserRole;
import com.phuchoang2005.aitraining.domain.Enums.UserStatus;
import com.phuchoang2005.aitraining.domain.Models.Project;
import com.phuchoang2005.aitraining.domain.Models.TrainingJob;
import com.phuchoang2005.aitraining.domain.Models.User;
import com.phuchoang2005.aitraining.domain.Enums.JobStatus;
import com.phuchoang2005.aitraining.domain.Enums.SourceType;
import com.phuchoang2005.aitraining.repo.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorizationServiceTest {

  @Mock
  ProjectRepository projectRepository;

  AuthorizationService authz;

  UUID ownerId = UUID.randomUUID();
  UUID otherId = UUID.randomUUID();
  UUID projectId = UUID.randomUUID();

  User owner;
  User otherUser;
  User admin;
  Project project;

  @BeforeEach
  void setUp() {
    authz = new AuthorizationService(projectRepository);
    owner = new User(ownerId, "owner@example.com", "Owner", UserRole.USER, UserStatus.ACTIVE, Instant.now(), null);
    otherUser = new User(otherId, "other@example.com", "Other", UserRole.USER, UserStatus.ACTIVE, Instant.now(), null);
    admin = new User(UUID.randomUUID(), "admin@example.com", "Admin", UserRole.ADMIN, UserStatus.ACTIVE, Instant.now(), null);
    project = new Project(projectId, ownerId, "Test", null, SourceType.GITHUB, null, null, "train.py", "ACTIVE", "READY", null, Instant.now(), Instant.now());
    when(projectRepository.get(projectId)).thenReturn(project);
  }

  @Test
  void requireProjectOwner_allowsOwner() {
    assertThat(authz.requireProjectOwner(owner, projectId)).isEqualTo(project);
  }

  @Test
  void requireProjectOwner_rejectsNonOwner() {
    assertThatThrownBy(() -> authz.requireProjectOwner(otherUser, projectId))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void requireProjectOwner_rejectsAdmin() {
    assertThatThrownBy(() -> authz.requireProjectOwner(admin, projectId))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void requireProjectVisible_allowsOwner() {
    assertThat(authz.requireProjectVisible(owner, projectId)).isEqualTo(project);
  }

  @Test
  void requireProjectVisible_allowsAdmin() {
    assertThat(authz.requireProjectVisible(admin, projectId)).isEqualTo(project);
  }

  @Test
  void requireProjectVisible_rejectsNonOwnerNonAdmin() {
    assertThatThrownBy(() -> authz.requireProjectVisible(otherUser, projectId))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void requireSensitiveJobOwner_rejectsAdmin() {
    TrainingJob job = buildJob(projectId);
    assertThatThrownBy(() -> authz.requireSensitiveJobOwner(admin, job))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("Ownership");
  }

  @Test
  void requireSensitiveJobOwner_allowsOwner() {
    TrainingJob job = buildJob(projectId);
    assertThat(authz.requireSensitiveJobOwner(owner, job)).isEqualTo(project);
  }

  @Test
  void requireAdmin_throwsForUser() {
    assertThatThrownBy(() -> authz.requireAdmin(owner))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void requireAdmin_passesForAdmin() {
    authz.requireAdmin(admin); // no exception
  }

  private TrainingJob buildJob(UUID pId) {
    return new TrainingJob(UUID.randomUUID(), pId, ownerId, UUID.randomUUID(), null,
        JobStatus.QUEUED, 0, null, Instant.now(), null, null, null, Instant.now());
  }
}
