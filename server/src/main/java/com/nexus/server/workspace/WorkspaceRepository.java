package com.nexus.server.workspace;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


// Tells Spring Boot that this class is responsible for data access
@Repository
public class WorkspaceRepository {

    // Primary route for executing SQL against PostgreSQL
    private final JdbcTemplate jdbc;

    private static final RowMapper<Workspace> MAPPER =
            (rs, rowNum) -> new Workspace(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    rs.getString("slug"),
                    rs.getTimestamp("created_at").toInstant()
            );

    public WorkspaceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Workspace create(String name, String slug) {
        String sql = """
                INSERT INTO workspaces (name, slug)
                VALUES (?, ?)
                RETURNING id, name, slug, created_at
                """;

        return jdbc.queryForObject(sql, MAPPER, name, slug);
    }

    public Optional<Workspace> findById(UUID id) {
        String sql = """
                SELECT id, name, slug, created_at
                FROM workspaces
                WHERE id = ?
                """;

        List<Workspace> results = jdbc.query(sql, MAPPER, id);

        return results.stream().findFirst();
    }

    public Optional<Workspace> findBySlug(String slug) {
        String sql = """
                SELECT id, name, slug, created_at
                FROM workspaces
                WHERE slug = ?
                """;

        List<Workspace> results = jdbc.query(sql, MAPPER, slug);

        return results.stream().findFirst();
    }

}
