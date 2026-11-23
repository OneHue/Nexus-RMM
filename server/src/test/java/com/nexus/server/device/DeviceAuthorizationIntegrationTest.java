
package com.nexus.server.device;

import com.nexus.server.device.repository.DeviceRepository;
import com.nexus.server.user.User;
import com.nexus.server.user.repository.UserRepository;

import com.nexus.server.workspace.Workspace;
import com.nexus.server.workspace.repository.WorkspaceRepository;
import com.nexus.server.workspace.repository.WorkspaceMembershipRepository;
import com.nexus.server.workspace.WorkspaceRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeviceAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private WorkspaceRepository workspaces;

    @Autowired
    private WorkspaceMembershipRepository memberships;

    @Autowired
    private DeviceRepository devices;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JdbcTemplate jdbc;

    private static final String PASSWORD =
            "Nexus-Device-Test-Passphrase-123!";

    private User createUser(String prefix) {
        return users.create(
                prefix + "-" + UUID.randomUUID()
                        + "@example.test",
                prefix,
                encoder.encode(PASSWORD)
        );
    }

    private Workspace createWorkspace(String prefix) {
        return workspaces.create(
                prefix + " Infrastructure",
                prefix + "-" + UUID.randomUUID()
        );
    }

    private MockHttpSession login(User user) throws Exception {

        MvcResult result = mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", user.email())
                                .param("password", PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result.getRequest()
                        .getSession(false);

        assertNotNull(session);
        return session;
    }

    @Test
    void membersCanReadOnlyTheirWorkspaceDevices()
            throws Exception {

        User viewer = createUser("viewer");
        User outsider = createUser("outsider");

        Workspace home = createWorkspace("home");
        Workspace business = createWorkspace("business");

        memberships.create(
                home.id(), viewer.id(), WorkspaceRole.VIEWER
        );

        memberships.create(
                business.id(), outsider.id(), WorkspaceRole.ADMIN
        );

        Device pc = devices.create(
                home.id(),
                "Gaming Desktop",
                "DESKTOP-01",
                "Windows 11"
        );

        MockHttpSession viewerSession = login(viewer);

        // Viewer can list devices in their workspace.
        mvc.perform(
                        get("/api/v1/workspaces/{w}/devices", home.id())
                                .session(viewerSession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(pc.id().toString()))
                .andExpect(jsonPath("$[0].name")
                        .value("Gaming Desktop"));

        // Viewer can retrieve an individual device.
        mvc.perform(
                        get(
                                "/api/v1/workspaces/{w}/devices/{d}",
                                home.id(),
                                pc.id()
                        ).session(viewerSession)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hostname")
                        .value("DESKTOP-01"));

        MockHttpSession outsiderSession = login(outsider);

        // An administrator elsewhere cannot read Home.
        mvc.perform(
                        get(
                                "/api/v1/workspaces/{w}/devices/{d}",
                                home.id(),
                                pc.id()
                        ).session(outsiderSession)
                )
                .andExpect(status().isForbidden());

        // Supplying another workspace with the same
        // device UUID also cannot retrieve the device.
        mvc.perform(
                        get(
                                "/api/v1/workspaces/{w}/devices/{d}",
                                business.id(),
                                pc.id()
                        ).session(outsiderSession)
                )
                .andExpect(status().isNotFound());

        // Anonymous users cannot list devices.
        mvc.perform(
                        get("/api/v1/workspaces/{w}/devices", home.id())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyOwnersAndAdminsCanRenameDevices()
            throws Exception {

        User owner = createUser("owner");
        User viewer = createUser("viewer");

        Workspace home = createWorkspace("home");

        memberships.create(
                home.id(), owner.id(), WorkspaceRole.OWNER
        );

        memberships.create(
                home.id(), viewer.id(), WorkspaceRole.VIEWER
        );

        Device pc = devices.create(
                home.id(),
                "Main PC",
                "DESKTOP-01",
                "Windows 11"
        );

        MockHttpSession ownerSession = login(owner);
        MockHttpSession viewerSession = login(viewer);

        String path = "/api/v1/workspaces/"
                + home.id()
                + "/devices/"
                + pc.id();

        // Viewer cannot rename devices.
        mvc.perform(
                        patch(path)
                                .session(viewerSession)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {"name":"Unauthorized Name"}
                            """)
                )
                .andExpect(status().isForbidden());

        // Owner can rename devices.
        mvc.perform(
                        patch(path)
                                .session(ownerSession)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {"name":"Development PC"}
                            """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Development PC"));

        // Verify the database actually changed.
        assertEquals(
                "Development PC",
                devices.findById(home.id(), pc.id())
                        .orElseThrow()
                        .name()
        );

        // Empty names are rejected.
        mvc.perform(
                        patch(path)
                                .session(ownerSession)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {"name":"   "}
                            """)
                )
                .andExpect(status().isBadRequest());

        // Missing devices return 404.
        mvc.perform(
                        get(
                                "/api/v1/workspaces/{w}/devices/{d}",
                                home.id(),
                                UUID.randomUUID()
                        ).session(ownerSession)
                )
                .andExpect(status().isNotFound());

        // Device creation remains disabled.
        mvc.perform(
                        post("/api/v1/workspaces/{w}/devices", home.id())
                                .session(ownerSession)
                                .with(csrf())
                )
                .andExpect(status().isForbidden());

        // Device deletion remains disabled.
        mvc.perform(
                        delete(path)
                                .session(ownerSession)
                                .with(csrf())
                )
                .andExpect(status().isForbidden());


        User admin = createUser("admin");

        memberships.create(
                home.id(),
                admin.id(),
                WorkspaceRole.ADMIN
        );

        MockHttpSession adminSession = login(admin);

// Administrators can rename devices.
        mvc.perform(
                        patch(path)
                                .session(adminSession)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"name":"Admin Renamed PC"}
                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Admin Renamed PC"));

// Even an owner cannot modify a device
// without supplying a valid CSRF token.
        mvc.perform(
                        patch(path)
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"name":"Invalid CSRF Request"}
                        """)
                )
                .andExpect(status().isForbidden());

    }

    @Test
    void disabledUsersCannotAccessDevices()
            throws Exception {

        User owner = createUser("disabled");
        Workspace home = createWorkspace("home");

        memberships.create(
                home.id(), owner.id(), WorkspaceRole.OWNER
        );

        MockHttpSession session = login(owner);

        // Disable the account after login.
        jdbc.update(
                """
                UPDATE users
                SET disabled_at = NOW(),
                    updated_at = NOW()
                WHERE id = ?
                """,
                owner.id()
        );

        mvc.perform(
                        get("/api/v1/workspaces/{w}/devices", home.id())
                                .session(session)
                )
                .andExpect(status().isForbidden());
    }
}