package com.example.aitraining.repo;

import com.example.aitraining.domain.Enums.SourceType;
import com.example.aitraining.domain.Models.Project;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class ProjectRepository {
    private final JdbcTemplate jdbc;

    public ProjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Project create(UUID ownerId, String name, String description, SourceType sourceType,
                          String repositoryUrl, String localPath, String entrypoint) {
        return jdbc.queryForObject("""
                INSERT INTO projects (owner_user_id, project_name, description, source_type, repository_url,
                  local_source_path, training_entrypoint)
                VALUES (?, ?, ?, ?::source_type, ?, ?, ?) RETURNING *
                """, this::map, ownerId, name, description, sourceType.name(), repositoryUrl, localPath, entrypoint);
    }

    public Project get(UUID projectId) {
        return jdbc.queryForObject("SELECT * FROM projects WHERE project_id = ?", this::map, projectId);
    }

    public List<Project> listVisible(UUID userId, boolean admin, String query, int limit) {
        String search = query == null || query.isBlank() ? "%" : "%" + query.toLowerCase() + "%";
        String sql = """
                SELECT * FROM projects
                WHERE (? OR owner_user_id = ?)
                  AND lower(project_name) LIKE ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        return jdbc.query(sql, this::map, admin, userId, search, limit);
    }

    public void delete(UUID projectId) {
        jdbc.update("DELETE FROM projects WHERE project_id = ?", projectId);
    }

    private Project map(ResultSet rs, int rowNum) throws SQLException {
        return new Project(
                rs.getObject("project_id", UUID.class),
                rs.getObject("owner_user_id", UUID.class),
                rs.getString("project_name"),
                rs.getString("description"),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("repository_url"),
                rs.getString("local_source_path"),
                rs.getString("training_entrypoint"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
