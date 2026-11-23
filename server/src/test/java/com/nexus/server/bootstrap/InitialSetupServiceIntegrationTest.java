
package com.nexus.server.bootstrap;

import com.nexus.server.user.User;
import com.nexus.server.user.UserRepository;
import com.nexus.server.workspace.WorkspaceMembershipRepository;
import com.nexus.server.workspace.WorkspaceRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class InitialSetupIntegrationTest {

    @Autowired
    private InitialSetupService setup;

    @Autowired
    private UserRepository users;

    @Autowired
    private WorkspaceMembershipRepository memberships;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder encoder;

    @Test
    void createsInitialOwnerAndWorkspace() {

        String email = "owner-" + UUID.randomUUID()
                + "@example.test";

        String password = "Long-Example-Passphrase-123!";

        var result = setup.initialize(
                email,
                "Nexus Administrator",
                password,
                "Home Infrastructure",
                "home-infrastructure"
        );

        User owner = users.findById(result.userId())
                .orElseThrow();

        assertEquals(email, owner.email());

        var membership = memberships
                .findByWorkspaceAndUser(
                        result.workspaceId(),
                        owner.id()
                )
                .orElseThrow();

        assertEquals(
                WorkspaceRole.OWNER,
                membership.role()
        );

        String storedHash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?",
                String.class,
                owner.id()
        );

        assertNotNull(storedHash);
        assertNotEquals(password, storedHash);
        assertTrue(storedHash.startsWith("{argon2id}"));
        assertTrue(encoder.matches(password, storedHash));

        Timestamp initialized = jdbc.queryForObject(
                """
                SELECT initialized_at
                FROM nexus_installation
                WHERE id = 1
                """,
                Timestamp.class
        );

        assertNotNull(initialized);

        // Attempting initialization again must fail.
        assertThrows(
                IllegalStateException.class,
                () -> setup.initialize(
                        "another@example.test",
                        "Another Owner",
                        password,
                        "Another Workspace",
                        "another-workspace"
                )
        );
    }
}
