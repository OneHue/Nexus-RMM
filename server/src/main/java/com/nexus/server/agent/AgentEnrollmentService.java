
package com.nexus.server.agent;

import com.nexus.server.device.Device;
import com.nexus.server.device.repository.DeviceRepository;
import com.nexus.server.security.service.CurrentUserService;
import com.nexus.server.user.User;

import com.nexus.server.workspace.WorkspaceRole;
import com.nexus.server.workspace.service.WorkspaceMembershipService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class AgentEnrollmentService {

    private final JdbcTemplate jdbc;
    private final DeviceRepository devices;
    private final CurrentUserService currentUsers;
    private final WorkspaceMembershipService memberships;

    public AgentEnrollmentService(
            JdbcTemplate jdbc,
            DeviceRepository devices,
            CurrentUserService currentUsers,
            WorkspaceMembershipService memberships
    ) {
        this.jdbc = jdbc;
        this.devices = devices;
        this.currentUsers = currentUsers;
        this.memberships = memberships;
    }

    @Transactional
    public Invitation createInvitation(
            UUID workspaceId,
            String deviceName,
            Authentication authentication
    ) {
        User user = currentUsers.requireCurrentUser(
                authentication
        );

        memberships.requireRole(
                user.id(),
                workspaceId,
                EnumSet.of(
                        WorkspaceRole.OWNER,
                        WorkspaceRole.ADMIN
                )
        );

        String name = requireText(
                deviceName, 120, "Device name"
        );

        String code = AgentTokenUtil.generate("nxe_");

        Instant expiresAt = Instant.now()
                .plus(10, ChronoUnit.MINUTES);

        jdbc.update("""
                INSERT INTO agent_enrollment_codes (
                    workspace_id,
                    created_by_user_id,
                    device_name,
                    code_hash,
                    expires_at
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                workspaceId,
                user.id(),
                name,
                AgentTokenUtil.hash(code),
                Timestamp.from(expiresAt)
        );

        return new Invitation(code, expiresAt);
    }

    @Transactional
    public EnrollmentResult enroll(
            String code,
            String hostname,
            String operatingSystem
    ) {
        if (code == null ||
                !code.matches("nxe_[A-Za-z0-9_-]{43}")) {
            throw new InvalidEnrollmentException();
        }

        String validHostname = optionalText(hostname, 255);
        String validOS = optionalText(operatingSystem, 120);

        // PostgreSQL atomically consumes the invitation.
        List<EnrollmentTarget> targets = jdbc.query("""
                UPDATE agent_enrollment_codes
                SET used_at = NOW()
                WHERE code_hash = ?
                  AND used_at IS NULL
                  AND expires_at > NOW()
                RETURNING workspace_id, device_name
                """,
                (rs, rowNum) -> new EnrollmentTarget(
                        rs.getObject(
                                "workspace_id", UUID.class
                        ),
                        rs.getString("device_name")
                ),
                AgentTokenUtil.hash(code)
        );

        if (targets.size() != 1) {
            throw new InvalidEnrollmentException();
        }

        EnrollmentTarget target = targets.getFirst();

        Device device = devices.create(
                target.workspaceId(),
                target.deviceName(),
                validHostname,
                validOS
        );

        String credential = AgentTokenUtil.generate("nxa_");

        jdbc.update("""
                INSERT INTO agent_credentials (
                    device_id,
                    secret_hash
                )
                VALUES (?, ?)
                """,
                device.id(),
                AgentTokenUtil.hash(credential)
        );

        return new EnrollmentResult(
                device.id(),
                target.workspaceId(),
                credential
        );
    }

    private static String requireText(
            String value, int max, String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        String result = value.trim();

        if (result.length() > max) {
            throw new IllegalArgumentException(
                    field + " is too long"
            );
        }

        return result;
    }

    private static String optionalText(
            String value, int max
    ) {
        return value == null || value.isBlank()
                ? null
                : requireText(value, max, "Field");
    }

    private record EnrollmentTarget(
            UUID workspaceId,
            String deviceName
    ) {
    }

    public record Invitation(
            String code,
            Instant expiresAt
    ) {
    }

    public record EnrollmentResult(
            UUID deviceId,
            UUID workspaceId,
            String credential
    ) {
    }

    public static class InvalidEnrollmentException
            extends RuntimeException {
    }
}
