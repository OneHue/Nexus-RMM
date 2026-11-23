
package com.nexus.server.security;

import com.nexus.server.user.User;
import com.nexus.server.user.repository.UserRepository;
import com.nexus.server.workspace.*;
import com.nexus.server.workspace.repository.WorkspaceMembershipRepository;
import com.nexus.server.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkspaceAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private WorkspaceRepository workspaces;

    @Autowired
    private WorkspaceMembershipRepository memberships;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JdbcTemplate jdbc;

    private static final String TEST_PASSWORD =
            "Nexus-Integration-Test-Password-123!";

    private User createUser(String prefix) {
        return users.create(
                prefix + "-" + UUID.randomUUID() + "@example.test",
                prefix,
                encoder.encode(TEST_PASSWORD)
        );
    }

    private MockHttpSession login(User user) throws Exception {

        MvcResult result = mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", user.email())
                                .param("password", TEST_PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        return (MockHttpSession) result.getRequest()
                .getSession(false);
    }

    @Test
    void enforcesWorkspaceIsolationAndRoles() throws Exception {

        User owner = createUser("owner");
        User viewer = createUser("viewer");
        User outsider = createUser("outsider");

        Workspace home = workspaces.create(
                "Home Infrastructure",
                "home-" + UUID.randomUUID()
        );

        Workspace business = workspaces.create(
                "Business Infrastructure",
                "business-" + UUID.randomUUID()
        );

        memberships.create(
                home.id(), owner.id(), WorkspaceRole.OWNER
        );

        memberships.create(
                home.id(), viewer.id(), WorkspaceRole.VIEWER
        );

        memberships.create(
                business.id(), outsider.id(), WorkspaceRole.ADMIN
        );

        MockHttpSession viewerSession = login(viewer);

        // Viewer sees only their own workspace.
        mvc.perform(
                        get("/api/v1/workspaces")
                                .session(viewerSession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(home.id().toString()))
                .andExpect(jsonPath("$[0].role")
                        .value("VIEWER"))
                .andExpect(jsonPath("$[1]").doesNotExist());

        // Viewer can read their workspace.
        mvc.perform(
                        get("/api/v1/workspaces/{id}", home.id())
                                .session(viewerSession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Home Infrastructure"));

        // Viewer cannot read administrative membership data.
        mvc.perform(
                        get("/api/v1/workspaces/{id}/members", home.id())
                                .session(viewerSession)
                )
                .andExpect(status().isForbidden());

        // Owner can read workspace memberships.
        MockHttpSession ownerSession = login(owner);

        mvc.perform(
                        get("/api/v1/workspaces/{id}/members", home.id())
                                .session(ownerSession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Admin of another workspace has no access to Home.
        MockHttpSession outsiderSession = login(outsider);

        mvc.perform(
                        get("/api/v1/workspaces/{id}", home.id())
                                .session(outsiderSession)
                )
                .andExpect(status().isForbidden());

        // Even owners cannot use unfinished write endpoints.
        mvc.perform(
                        post("/api/v1/workspaces")
                                .session(ownerSession)
                                .with(csrf())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledAccountLosesWorkspaceAccess() throws Exception {

        User user = createUser("disabled");

        Workspace workspace = workspaces.create(
                "Test Workspace",
                "test-" + UUID.randomUUID()
        );

        memberships.create(
                workspace.id(),
                user.id(),
                WorkspaceRole.OWNER
        );

        MockHttpSession session = login(user);

        // Disable the account after login.
        jdbc.update(
                """
                UPDATE users
                SET disabled_at = NOW(),
                    updated_at = NOW()
                WHERE id = ?
                """,
                user.id()
        );

        // The existing session no longer grants workspace access.
        mvc.perform(
                        get("/api/v1/workspaces")
                                .session(session)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnauthenticatedWorkspaceRequests() throws Exception {

        mvc.perform(
                        get("/api/v1/workspaces")
                )
                .andExpect(status().isUnauthorized());

        mvc.perform(
                        get("/api/v1/workspaces/{id}", UUID.randomUUID())
                )
                .andExpect(status().isUnauthorized());
    }
}
