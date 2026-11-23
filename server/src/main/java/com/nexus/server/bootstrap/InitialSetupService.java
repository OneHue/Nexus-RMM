package com.nexus.server.bootstrap;

import com.nexus.server.user.User;
import com.nexus.server.user.UserRepository;
import com.nexus.server.workspace.Workspace;
import com.nexus.server.workspace.WorkspaceMembershipRepository;
import com.nexus.server.workspace.WorkspaceRole;
import com.nexus.server.workspace.WorkspaceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class InitialSetupService {

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final WorkspaceService workspaces;
    private final WorkspaceMembershipRepository memberships;
    private final PasswordEncoder encoder;

    public InitialSetupService(
            JdbcTemplate jdbc,
            UserRepository users,
            WorkspaceService workspaces,
            WorkspaceMembershipRepository memberships,
            PasswordEncoder encoder
    ) {
        this.jdbc = jdbc;
        this.users = users;
        this.workspaces = workspaces;
        this.memberships = memberships;
        this.encoder = encoder;
    }

    public record SetupResult(
            UUID userId,
            UUID workspaceId
    ) {}

    @Transactional
    public SetupResult initialize(
            String email,
            String displayName,
            String password,
            String workspaceName,
            String workspaceSlug
    ) {
        String validEmail = validateEmail(email);
        String validName = requireText(displayName, 120);

        if (password == null || password.length() < 15 || password.length() > 1024) {
            throw new IllegalArgumentException(
                    "Password must be between 15 and 1024 characters"
            );
        }

        // Validate the installation input before any writes are made to the database.
        // This ensures that if any of the input is invalid, we don't end up with a partially initialized installation.
        requireText(workspaceName, 100);
        requireText(workspaceSlug, 100);

        // Compute the hash before acquiring the installation lock
        String hash = encoder.encode(password);

        // Only one transaction can claim initialization
        int claimed = jdbc.update("""
                UPDATE nexus_installation
                SET initialized_at = NOW()
                WHERE id = 1
                AND initialized_at IS NULL
                """);

        if (claimed != 1) {
            throw new IllegalStateException(
                    "Nexus has already been initialized"
            );
        }

        // Never claim ownership over pre-existing data
        Boolean hasUsers = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM users)", Boolean.class);
        Boolean hasWorkspaces = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM workspaces)", Boolean.class);

        if (Boolean.TRUE.equals(hasUsers) || Boolean.TRUE.equals(hasWorkspaces)) {
            throw new IllegalStateException("Existing installation data requires manual recovery");
        }

        // Create initial user with Argon2id hash
        User owner = users.create(
                validEmail,
                validName,
                hash
        );

        // Create initial workspace
        Workspace workspace = workspaces.createWorkspace(
                workspaceName,
                workspaceSlug
        );

        // Assign ownership
        memberships.create(
                workspace.id(),
                owner.id(),
                WorkspaceRole.OWNER
        );

        return new SetupResult(
                owner.id(),
                workspace.id()
        );

    }

    private static String validateEmail(String email) {
        String value = requireText(email, 254).toLowerCase(Locale.ROOT);

        if (!value.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("A valid email address is required");
        }

        return value;
    }

    private static String requireText(String text, int maxLength) {
        if (text == null || text.isBlank() || text.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Text must be between 1 and " + maxLength + " characters"
            );
        }
        return text.trim();
    }

}
