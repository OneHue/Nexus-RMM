
package com.nexus.server.identity;

import com.nexus.server.user.User;
import com.nexus.server.user.repository.UserRepository;

import com.nexus.server.workspace.Workspace;
import com.nexus.server.workspace.repository.WorkspaceRepository;
import com.nexus.server.workspace.WorkspaceMembership;
import com.nexus.server.workspace.repository.WorkspaceMembershipRepository;
import com.nexus.server.workspace.WorkspaceRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserMembershipIntegrationTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private WorkspaceRepository workspaces;

    @Autowired
    private WorkspaceMembershipRepository memberships;

    // Test fixture only. Never use this value for real accounts.
    private static final String TEST_HASH = "NOT_A_REAL_PASSWORD_HASH";

    @Test
    void canCreateUsersAndMemberships() {

        String email = "test-" + UUID.randomUUID()
                + "@example.test";

        User user = users.create(
                email,
                "Alex",
                TEST_HASH
        );

        assertNotNull(user.id());
        assertFalse(user.isDisabled());
        assertFalse(user.isEmailVerified());

        // Verify user retrieval.
        User found = users.findById(user.id())
                .orElseThrow();

        assertEquals(user, found);

        // Verify case-insensitive email lookup.
        assertEquals(
                user,
                users.findByEmail(
                        email.toUpperCase(Locale.ROOT)
                ).orElseThrow()
        );

        // Create two separate workspaces.
        Workspace home = workspaces.create(
                "Home Infrastructure",
                "home-" + UUID.randomUUID()
        );

        Workspace business = workspaces.create(
                "Business Infrastructure",
                "business-" + UUID.randomUUID()
        );

        // Assign different roles to the same user.
        WorkspaceMembership homeMembership =
                memberships.create(
                        home.id(),
                        user.id(),
                        WorkspaceRole.OWNER
                );

        WorkspaceMembership businessMembership =
                memberships.create(
                        business.id(),
                        user.id(),
                        WorkspaceRole.VIEWER
                );

        // Verify the roles.
        assertEquals(
                WorkspaceRole.OWNER,
                homeMembership.role()
        );

        assertEquals(
                WorkspaceRole.VIEWER,
                businessMembership.role()
        );

        // Verify membership retrieval.
        assertEquals(
                homeMembership,
                memberships.findByWorkspaceAndUser(
                        home.id(),
                        user.id()
                ).orElseThrow()
        );

        // Verify that one user can belong
        // to multiple workspaces.
        assertEquals(
                2,
                memberships.findAllByUser(user.id()).size()
        );

        // Verify workspace membership listing.
        assertTrue(
                memberships.findAllByWorkspace(home.id())
                        .contains(homeMembership)
        );
    }

    @Test
    void rejectsDuplicateEmailsIgnoringCase() {

        String email = "duplicate-" + UUID.randomUUID()
                + "@example.test";

        users.create(email, "First User", TEST_HASH);

        // This should violate our database unique index.
        assertThrows(
                DataIntegrityViolationException.class,
                () -> users.create(
                        email.toUpperCase(Locale.ROOT),
                        "Second User",
                        TEST_HASH
                )
        );
    }
}