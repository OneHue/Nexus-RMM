
package com.nexus.server.identity;

import com.nexus.server.user.User;
import com.nexus.server.user.repository.UserRepository;
import com.nexus.server.user.service.UserService;

import com.nexus.server.workspace.Workspace;
import com.nexus.server.workspace.repository.WorkspaceRepository;
import com.nexus.server.workspace.WorkspaceMembership;
import com.nexus.server.workspace.repository.WorkspaceMembershipRepository;
import com.nexus.server.workspace.service.WorkspaceMembershipService;
import com.nexus.server.workspace.WorkspaceRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class IdentityServiceIntegrationTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceRepository workspaces;

    @Autowired
    private WorkspaceMembershipRepository membershipRepository;

    @Autowired
    private WorkspaceMembershipService membershipService;

    @Autowired
    private JdbcTemplate jdbc;

    // Persistence test fixture only, never for real accounts.
    private static final String TEST_HASH = "NOT_A_REAL_HASH";

    @Test
    void enforcesWorkspaceRoles() {

        String suffix = UUID.randomUUID().toString();

        User owner = users.create(
                "owner-" + suffix + "@example.test",
                "Owner",
                TEST_HASH
        );

        User viewer = users.create(
                "viewer-" + suffix + "@example.test",
                "Viewer",
                TEST_HASH
        );

        Workspace workspace = workspaces.create(
                "Test Infrastructure",
                "test-" + suffix
        );

        Workspace otherWorkspace = workspaces.create(
                "Other Infrastructure",
                "other-" + suffix
        );

        WorkspaceMembership ownerMembership =
                membershipRepository.create(
                        workspace.id(),
                        owner.id(),
                        WorkspaceRole.OWNER
                );

        membershipRepository.create(
                workspace.id(),
                viewer.id(),
                WorkspaceRole.VIEWER
        );

        // User lookup works.
        assertEquals(
                owner,
                userService.getUser(owner.id())
        );

        assertEquals(
                owner,
                userService.findByEmail(
                        owner.email().toUpperCase(Locale.ROOT)
                ).orElseThrow()
        );

        // Owner is a member of the workspace.
        assertEquals(
                ownerMembership,
                membershipService.requireMembership(
                        owner.id(),
                        workspace.id()
                )
        );

        // Owner can access administrative membership data.
        assertEquals(
                2,
                membershipService.listWorkspaceMembers(
                        owner.id(),
                        workspace.id()
                ).size()
        );

        // Viewer cannot perform an administrator operation.
        assertThrows(
                AccessDeniedException.class,
                () -> membershipService.requireRole(
                        viewer.id(),
                        workspace.id(),
                        EnumSet.of(
                                WorkspaceRole.OWNER,
                                WorkspaceRole.ADMIN
                        )
                )
        );

        assertThrows(
                AccessDeniedException.class,
                () -> membershipService.listWorkspaceMembers(
                        viewer.id(),
                        workspace.id()
                )
        );

        // Owner cannot access an unrelated workspace.
        assertThrows(
                AccessDeniedException.class,
                () -> membershipService.requireMembership(
                        owner.id(),
                        otherWorkspace.id()
                )
        );

        // Workspace listing returns the user's memberships.
        assertTrue(
                membershipService.listUserMemberships(owner.id())
                        .contains(ownerMembership)
        );
    }

    @Test
    void disabledUsersCannotAccessWorkspaces() {

        String suffix = UUID.randomUUID().toString();

        User user = users.create(
                "disabled-" + suffix + "@example.test",
                "Disabled User",
                TEST_HASH
        );

        Workspace workspace = workspaces.create(
                "Disabled User Test",
                "disabled-" + suffix
        );

        membershipRepository.create(
                workspace.id(),
                user.id(),
                WorkspaceRole.OWNER
        );

        // Simulate an administrator disabling the account.
        jdbc.update(
                """
                UPDATE users
                SET disabled_at = NOW(),
                    updated_at = NOW()
                WHERE id = ?
                """,
                user.id()
        );

        assertThrows(
                AccessDeniedException.class,
                () -> userService.requireActiveUser(user.id())
        );

        assertThrows(
                AccessDeniedException.class,
                () -> membershipService.requireMembership(
                        user.id(),
                        workspace.id()
                )
        );
    }
}
