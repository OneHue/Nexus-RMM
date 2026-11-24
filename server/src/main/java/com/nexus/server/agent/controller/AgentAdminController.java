package com.nexus.server.agent.controller;


import com.nexus.server.agent.service.AgentCredentialService;
import com.nexus.server.agent.service.AgentEnrollmentService;
import com.nexus.server.security.service.CurrentUserService;
import com.nexus.server.workspace.WorkspaceRole;
import com.nexus.server.workspace.service.WorkspaceMembershipService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
public class AgentAdminController {

    private final AgentEnrollmentService enrollment;
    private final AgentCredentialService credentials;
    private final CurrentUserService currentUsers;
    private final WorkspaceMembershipService memberships;

    public AgentAdminController(
            AgentEnrollmentService enrollment,
            AgentCredentialService credentials,
            CurrentUserService currentUsers,
            WorkspaceMembershipService memberships
    ) {
        this.enrollment = enrollment;
        this.credentials = credentials;
        this.currentUsers = currentUsers;
        this.memberships = memberships;
    }

    public record InvitationRequest(
            String deviceName
    ) { }

    @PostMapping("/agent-enrollments")
    public ResponseEntity<AgentEnrollmentService.Invitation> createInvitation(
            @PathVariable UUID workspaceId,
            @RequestBody InvitationRequest request,
            Authentication authentication
            ) {
        try {
            var invitation = enrollment.createInvitation(
                    workspaceId,
                    request == null ? null : request.deviceName(),
                    authentication
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .cacheControl(CacheControl.noStore())
                    .body(invitation);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid device name");
        }
    }

    @PostMapping("/devices/{deviceId}/credentials/revoke")
    public ResponseEntity<Void> revokeCredential(
            @PathVariable UUID workspaceId,
            @PathVariable UUID deviceId,
            Authentication authentication
    ) {
        var user = currentUsers.requireCurrentUser(authentication);

        memberships.requireRole(
                user.id(),
                workspaceId,
                EnumSet.of(
                        WorkspaceRole.OWNER,
                        WorkspaceRole.ADMIN
                )
        );

        credentials.revoke(workspaceId, deviceId);

        return ResponseEntity.noContent().build();
    }

}
