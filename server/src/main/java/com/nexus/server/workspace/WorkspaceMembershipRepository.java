package com.nexus.server.workspace;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WorkspaceMembershipRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<WorkspaceMembership> MAPPER =
            (rs, rowNum) -> new WorkspaceMembership(
                    rs.getObject("workspace_id", UUID.class),
                    rs.getObject("user_id", UUID.class),

                    // Casts the role string from the database into the corresponding enum
                    WorkspaceRole.valueOf(
                            rs.getString("role")
                    ),

                    rs.getTimestamp("created_at").toInstant()
            );

    public WorkspaceMembershipRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public WorkspaceMembership create(
            UUID workspaceId,
            UUID userId,
            WorkspaceRole role
    ) {
        String sql = """
                INSERT INTO workspace_memberships (
                    workspace_id,
                    user_id,
                    role
                )
                VALUES (?, ?, ?)
                RETURNING *
                """;

        return jdbc.queryForObject(
                sql,
                MAPPER,
                workspaceId,
                userId,

                // retrieves the string of the role enum
                role.name()
        );
    }

    public Optional<WorkspaceMembership> findByWorkspaceAndUser(
            UUID workspaceId,
            UUID userId
    ) {
        String sql = """
                SELECT *
                FROM workspace_memberships
                WHERE workspace_id = ?
                    AND user_id = ?
                """;

        List<WorkspaceMembership> results = jdbc.query(
                sql,
                MAPPER,
                workspaceId,
                userId
        );

        return results.stream().findFirst();
    }

    public List<WorkspaceMembership> findAllByUser(
            UUID userId
    ) {
        String sql = """
                SELECT *
                FROM workspace_memberships
                WHERE user_id = ?
                ORDER BY workspace_id
                """;

        return jdbc.query(sql, MAPPER, userId);
    }

    public List<WorkspaceMembership> findAllByWorkspace(
            UUID workspaceId
    ) {
        String sql = """
                SELECT *
                FROM workspace_memberships
                WHERE workspace_id = ?
                ORDER BY user_id
                """;

        return jdbc.query(sql, MAPPER, workspaceId);
    }
}
