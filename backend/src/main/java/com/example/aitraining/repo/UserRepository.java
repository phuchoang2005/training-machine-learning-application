package com.example.aitraining.repo;

import com.example.aitraining.domain.Enums.UserRole;
import com.example.aitraining.domain.Enums.UserStatus;
import com.example.aitraining.domain.Models.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findActiveByToken(String token) {
        String sql = """
                SELECT * FROM users
                WHERE status = 'ACTIVE' AND (lower(email) = lower(?) OR user_id::text = ?)
                """;
        return jdbc.query(sql, this::map, token, token).stream().findFirst();
    }

    public User get(UUID userId) {
        return jdbc.queryForObject("SELECT * FROM users WHERE user_id = ?", this::map, userId);
    }

    public List<User> list(int limit) {
        return jdbc.query("SELECT * FROM users ORDER BY created_at LIMIT ?", this::map, limit);
    }

    public User updateStatus(UUID userId, UserStatus status) {
        return jdbc.queryForObject("""
                UPDATE users SET status = ?::user_status WHERE user_id = ? RETURNING *
                """, this::map, status.name(), userId);
    }

    private User map(ResultSet rs, int rowNum) throws SQLException {
        return new User(
                rs.getObject("user_id", UUID.class),
                rs.getString("email"),
                rs.getString("full_name"),
                UserRole.valueOf(rs.getString("role")),
                UserStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toInstant());
    }
}
