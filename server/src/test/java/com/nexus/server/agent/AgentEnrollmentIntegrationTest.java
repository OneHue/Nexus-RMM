
package com.nexus.server.agent;

import com.nexus.server.agent.controller.AgentProtocolController;
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

import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgentEnrollmentIntegrationTest {

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

    private final JsonMapper json =
            JsonMapper.builder().build();

    private static final String PASSWORD =
            "Nexus-Agent-Test-Passphrase-123!";

    @Test
    void enrollsOnceAuthenticatesAndRevokes()
            throws Exception {

        User owner = users.create(
                "agent-owner-" + UUID.randomUUID()
                        + "@example.test",
                "Agent Owner",
                encoder.encode(PASSWORD)
        );

        Workspace workspace = workspaces.create(
                "Agent Test Workspace",
                "agent-" + UUID.randomUUID()
        );

        memberships.create(
                workspace.id(),
                owner.id(),
                WorkspaceRole.OWNER
        );

        MvcResult login = mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", owner.email())
                                .param("password", PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) login
                        .getRequest().getSession(false);

        assertNotNull(session);

        // Authorized administrator creates invitation.
        MvcResult invitation = mvc.perform(
                        post(
                                "/api/v1/workspaces/{w}/agent-enrollments",
                                workspace.id()
                        )
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"deviceName":"Test Workstation"}
                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String code = json.readTree(
                invitation.getResponse().getContentAsString()
        ).path("code").asText();

        assertTrue(code.startsWith("nxe_"));

        String enrollmentJson = json.writeValueAsString(
                new AgentProtocolController.EnrollmentRequest(
                        code,
                        "DESKTOP-TEST",
                        "Windows"
                )
        );

        // Agent exchanges the code for a credential.
        MvcResult enrollment = mvc.perform(
                        post("/api/v1/agent/enroll")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(enrollmentJson)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        var result = json.readTree(
                enrollment.getResponse().getContentAsString()
        );

        String credential =
                result.path("credential").asText();

        UUID deviceId = UUID.fromString(
                result.path("deviceId").asText()
        );

        assertTrue(credential.startsWith("nxa_"));

        assertEquals(
                workspace.id().toString(),
                result.path("workspaceId").asText()
        );

        // Enrollment code cannot be reused.
        mvc.perform(
                        post("/api/v1/agent/enroll")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(enrollmentJson)
                )
                .andExpect(status().isBadRequest());

        // No credential = no heartbeat.
        mvc.perform(
                        post("/api/v1/agent/heartbeat")
                )
                .andExpect(status().isUnauthorized());

        // Invalid credential = no heartbeat.
        mvc.perform(
                        post("/api/v1/agent/heartbeat")
                                .header("Authorization", "Bearer invalid")
                )
                .andExpect(status().isUnauthorized());

        // Valid credential identifies the enrolled device.
        mvc.perform(
                        post("/api/v1/agent/heartbeat")
                                .header(
                                        "Authorization",
                                        "Bearer " + credential
                                )
                )
                .andExpect(status().isNoContent());

        var lastSeen = jdbc.queryForObject(
                """
                SELECT last_seen_at
                FROM devices
                WHERE id = ?
                """,
                java.sql.Timestamp.class,
                deviceId
        );

        assertNotNull(lastSeen);

        // Administrator revokes the agent credential.
        mvc.perform(
                        post(
                                "/api/v1/workspaces/{w}/devices/{d}/credentials/revoke",
                                workspace.id(),
                                deviceId
                        )
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isNoContent());

        // Revoked credential is rejected.
        mvc.perform(
                        post("/api/v1/agent/heartbeat")
                                .header(
                                        "Authorization",
                                        "Bearer " + credential
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCannotIssueEnrollmentCodes()
            throws Exception {

        User viewer = users.create(
                "agent-viewer-" + UUID.randomUUID()
                        + "@example.test",
                "Agent Viewer",
                encoder.encode(PASSWORD)
        );

        Workspace workspace = workspaces.create(
                "Viewer Workspace",
                "viewer-" + UUID.randomUUID()
        );

        memberships.create(
                workspace.id(),
                viewer.id(),
                WorkspaceRole.VIEWER
        );

        MvcResult login = mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", viewer.email())
                                .param("password", PASSWORD)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) login
                        .getRequest().getSession(false);

        assertNotNull(session);

        mvc.perform(
                        post(
                                "/api/v1/workspaces/{w}/agent-enrollments",
                                workspace.id()
                        )
                                .session(session)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"deviceName":"Unauthorized Device"}
                        """)
                )
                .andExpect(status().isForbidden());
    }
}
