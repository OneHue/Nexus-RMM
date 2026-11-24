package com.nexus.server.agent;

import com.nexus.server.device.service.DeviceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgentCredentialService {

    private final JdbcTemplate jdbc;
    private final DeviceService devices;

    public AgentCredentialService(JdbcTemplate jdbc, DeviceService devices) {
        this.jdbc = jdbc;
        this.devices = devices;
    }

    @Transactional(readOnly = true)
    public Optional<AgentPrincipal> authenticate(String token) {

        if (token == null || !token.matches("nxa_[A-Za-z0-9_-]{43}")) {
            return Optional.empty();
        }

        List<AgentPrincipal> agents = jdbc.query("""
                SELECT
                    d.id AS device_id,
                    d.workspace_id
                FROM agent_credentials c
                JOIN devices d ON d.id = c.device_id
                WHERE c.secret_hash = ?
                    AND c.revoked_at IS NULL
                """,
                (rs, rowNum) -> new AgentPrincipal(
                        rs.getObject("device_id", UUID.class),
                        rs.getObject("workspace_id", UUID.class)
                ),
                AgentTokenUtil.hash(token)
        );

        return agents.stream().findFirst();

    }

    @Transactional
    public void revoke(UUID workspaceId, UUID deviceId) {
        // Verify the device belongs to the specified workspace
        devices.getDevice(workspaceId, deviceId);

        jdbc.update("""
                UPDATE agent_credentials
                SET revoked_at = NOW()
                WHERE device_id = ?
                    AND revoked_at IS NULL
                """,
                deviceId);
    }

}
