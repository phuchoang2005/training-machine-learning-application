package com.example.aitraining.repo;

import com.example.aitraining.domain.Models.ProjectConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Repository
public class ConfigRepository {
    private final JdbcTemplate jdbc;

    public ConfigRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ProjectConfig createDefault(UUID projectId, String entrypoint) {
        String yaml = "trainingEntrypoint: " + entrypoint + "\n";
        return jdbc.queryForObject("""
                INSERT INTO project_configs (project_id, config_name, config_path, yaml_content, default_config)
                VALUES (?, 'default', 'configs/default.yaml', ?, true) RETURNING *
                """, this::map, projectId, yaml);
    }

    public List<ProjectConfig> list(UUID projectId) {
        return jdbc.query("SELECT * FROM project_configs WHERE project_id = ? ORDER BY default_config DESC, config_path",
                this::map, projectId);
    }

    public ProjectConfig get(UUID projectId, UUID configId) {
        return jdbc.queryForObject("SELECT * FROM project_configs WHERE project_id = ? AND config_id = ?",
                this::map, projectId, configId);
    }

    public UUID createSnapshot(UUID projectId, UUID configId, String yamlContent) {
        String hash = sha256(yamlContent);
        return jdbc.queryForObject("""
                INSERT INTO config_snapshots (project_id, config_id, yaml_content, content_hash)
                VALUES (?, ?, ?, ?) RETURNING snapshot_id
                """, UUID.class, projectId, configId, yamlContent, hash);
    }

    public String contentHash(String yamlContent) {
        return sha256(yamlContent);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash YAML content", ex);
        }
    }

    private ProjectConfig map(ResultSet rs, int rowNum) throws SQLException {
        return new ProjectConfig(
                rs.getObject("config_id", UUID.class),
                rs.getObject("project_id", UUID.class),
                rs.getString("config_name"),
                rs.getString("config_path"),
                rs.getString("yaml_content"),
                rs.getBoolean("default_config"),
                rs.getTimestamp("updated_at").toInstant());
    }
}
