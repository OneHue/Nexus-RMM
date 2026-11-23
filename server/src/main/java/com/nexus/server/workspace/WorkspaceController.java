package com.nexus.server.workspace;


import com.nexus.server.security.CurrentUserService;
import com.nexus.server.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final CurrentUserService currentUsers;
    private final WorkspaceMembershipService memberships;
    private final WorkspaceService workspaces;

    public WorkspaceController(
            CurrentUserService currentUsers,
            WorkspaceMembershipService memberships,
            WorkspaceService workspaces
    ) {
        this.currentUsers = currentUsers;
        this.memberships = memberships;
        this.workspaces = workspaces;
    }

    public record WorkspaceResponse(
            UUID id,
            String name,
            String slug,
            WorkspaceRole role
    ) { }

    public record MemberResponse(
            UUID userId,
            WorkspaceRole role
    ) { }

    private static WorkspaceResponse toResponse(
            Workspace workspace,
            WorkspaceRole role
    ) {
        return new WorkspaceResponse(
                workspace.id(),
                workspace.name(),
                workspace.slug(),
                role
        );
    }

    @GetMapping
    public List<WorkspaceResponse> listWorkspaces(Authentication authentication) {
        User user = currentUsers.requireCurrentUser(authentication);

        return memberships.listUserMemberships(user.id())
                .stream()
                .map(membership -> {
                    Workspace workspace = workspaces.getWorkspace(
                            membership.workspaceId()
                    );

                    return toResponse(
                            workspace,
                            membership.role()
                    );
                })
                .toList();
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse getWorkspace(
            @PathVariable UUID workspaceId,
            Authentication authentication
    ) {
        User user = currentUsers.requireCurrentUser(authentication);

        // Authorization happens before data retrieval
        WorkspaceMembership membership = memberships.requireMembership(user.id(), workspaceId);
        Workspace workspace = workspaces.getWorkspace(workspaceId);

        return toResponse(workspace, membership.role());
    }

    @GetMapping("/{workspaceId}/members")
    public List<MemberResponse> listMembers(
            @PathVariable UUID workspaceId,
            Authentication authentication
    ) {
        User user = currentUsers.requireCurrentUser(authentication);

        return memberships.listWorkspaceMembers(user.id(), workspaceId)
                .stream()
                .map(membership -> new MemberResponse(
                        membership.userId(),
                        membership.role()
                )).toList();
    }

}
