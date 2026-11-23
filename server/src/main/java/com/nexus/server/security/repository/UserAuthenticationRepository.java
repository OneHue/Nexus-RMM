package com.nexus.server.security.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserAuthenticationRepository {

    private final JdbcTemplate jdbc;

    public UserAuthenticationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserDetails> findByEmail(String email) {
        String sql = """
                SELECT
                    email,
                    password_hash,
                    disabled_at
                FROM users
                WHERE LOWER(email) = LOWER(?)
                """;

        List<UserDetails> results = jdbc.query(
                sql,
                (rs, rowNum) -> User
                        .withUsername(rs.getString("email"))
                        .password(rs.getString("password_hash"))
                        .disabled(rs.getTimestamp("disabled_at") != null)
                        .authorities("ROLE_NEXUS_USER")
                        .build(),
                email
        );

        return results.stream().findFirst();
    }

}
