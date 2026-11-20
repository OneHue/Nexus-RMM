package com.nexus.server.device;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceRepository {

    private  final JdbcTemplate jdbc;

    private static final RowMapper<Device> MAPPER =
            (rs, rowNum) -> {
                Timestamp lastSeen = rs.getTimestamp("last_seen_at");

                return new Device(
                        rs.getObject("id", UUID.class),
                        rs.getObject("workspace_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("hostname"),
                        rs.getString("operating_system"),
                        rs.getTimestamp("created_at").toInstant(),
                        lastSeen == null ? null : lastSeen.toInstant()
                );
            };

    public DeviceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Device create(
            UUID workspaceId,
            String name,
            String hostname,
            String operatingSystem
    ) {
        String sql = """
                INSERT INTO devices (
                    workspace_id,
                    name,
                    hostname,
                    operating_system
                )
                VALUES (?, ?, ?, ?)
                RETURNING *
                """;

        return jdbc.queryForObject(
                sql,
                MAPPER,
                workspaceId,
                name,
                hostname,
                operatingSystem
        );
    }

    public Optional<Device> findById(
            UUID workspaceId,
            UUID deviceId
    ) {
        String sql = """
                SELECT *
                FROM devices
                WHERE workspace_id = ?
                    AND id = ?
                """;

        List<Device> results = jdbc.query(
                sql, MAPPER, workspaceId, deviceId
        );

        return results.stream().findFirst();
    }

    public List<Device> findAllByWorkspaceId(
            UUID workspaceId
    ) {
        String sql = """
                SELECT *
                FROM devices
                WHERE workspace_id = ?
                ORDER BY created_at DESC, id
                """;

        return jdbc.query(sql, MAPPER, workspaceId);
    }

    public boolean rename(
            UUID workspaceId,
            UUID deviceId,
            String newName
    ) {
        String sql = """
                UPDATE devices
                SET name = ?
                WHERE workspace_id = ?
                    AND id = ?
                """;

        int rowsUpdated = jdbc.update(
                sql, newName, workspaceId, deviceId
        );

        return rowsUpdated == 1;
    }
}
