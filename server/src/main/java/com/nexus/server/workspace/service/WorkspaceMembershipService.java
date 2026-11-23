package com.nexus.server.workspace.service;

import com.nexus.server.user.service.UserService;
import com.nexus.server.workspace.WorkspaceMembership;
import com.nexus.server.workspace.WorkspaceRole;
import com.nexus.server.workspace.repository.WorkspaceMembershipRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkspaceMembershipService {

    private final WorkspaceMembershipRepository repository;
    private final UserService userService;

    public WorkspaceMembershipService(
            WorkspaceMembershipRepository repository,
            UserService userService
    ) {
        this.repository = repository;
        this.userService = userService;
    }


    // Returns a workspace membership if the user has an active account and the user has a membership in the requested workspace
    @Transactional(readOnly = true)
    public WorkspaceMembership requireMembership(
            UUID userId,
            UUID workspaceId
    ) {
        if (workspaceId == null) {
            throw new IllegalArgumentException(
                    "Workspace ID is required"
            );
        }

        // Ensure the user exists and isn't disabled
        userService.requireActiveUser(userId);

        return repository.findByWorkspaceAndUser(
                workspaceId,
                userId
        ).orElseThrow(() -> new AccessDeniedException(
                "Workspace access denied"
        ));
    }

    @Transactional(readOnly = true)
    public WorkspaceMembership requireRole(
            UUID userId,
            UUID workspaceId,
            Set<WorkspaceRole> allowedRoles
    ) {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one allowed role is required"
            );
        }

        WorkspaceMembership membership = requireMembership(userId, workspaceId);

        if (!allowedRoles.contains(membership.role())) {
            throw new AccessDeniedException(
                    "Insufficient workspace permissions"
            );
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMembership> listUserMemberships(
            UUID userId
    ) {
        userService.requireActiveUser(userId);

        return repository.findAllByUser(userId);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMembership> listWorkspaceMembers(
            UUID requestingUserId,
            UUID workspaceId
    ) {
        requireRole(
                requestingUserId,
                workspaceId,
                EnumSet.of(
                        WorkspaceRole.OWNER,
                        WorkspaceRole.ADMIN
                )
        );

        return repository.findAllByWorkspace(workspaceId);
    }
}
