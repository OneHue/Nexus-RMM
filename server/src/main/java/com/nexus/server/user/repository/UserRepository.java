package com.nexus.server.user.repository;

import com.nexus.server.user.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private  final JdbcTemplate jdbc;

    private static final RowMapper<User> MAPPER =
            (rs, rowNum) -> {
                Timestamp verified =
                        rs.getTimestamp("email_verified_at");

                Timestamp disabled =
                        rs.getTimestamp("disabled_at");

                return new User(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        verified == null ? null : verified.toInstant(),
                        disabled == null ? null : disabled.toInstant(),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                );
            };

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public User create(
            String email,
            String displayName,
            String passwordHash
    ) {
        String sql = """
                INSERT INTO users (
                    email,
                    display_name,
                    password_hash
                )
                VALUES (?, ?, ?)
                RETURNING
                    id,
                    email,
                    display_name,
                    email_verified_at,
                    disabled_at,
                    created_at,
                    updated_at
                """;

        return jdbc.queryForObject(
                sql,
                MAPPER,
                email,
                displayName,
                passwordHash
        );
    }

    public Optional<User> findById(UUID id) {
        String sql = """
                SELECT
                    id,
                    email,
                    display_name,
                    email_verified_at,
                    disabled_at,
                    created_at,
                    updated_at
                FROM users
                WHERE id = ?
                """;

        List<User> results = jdbc.query(
                sql,
                MAPPER,
                id
        );

        return results.stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {

        String sql = """
                SELECT
                    id,
                    email,
                    display_name,
                    email_verified_at,
                    disabled_at,
                    created_at,
                    updated_at
                FROM users
                WHERE LOWER(email) = LOWER(?)
                """;

        List<User> results = jdbc.query(
                sql,
                MAPPER,
                email
        );

        return results.stream().findFirst();

    }

}
