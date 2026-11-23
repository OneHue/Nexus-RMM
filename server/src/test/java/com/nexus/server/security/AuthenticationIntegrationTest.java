
package com.nexus.server.security;

import com.nexus.server.user.User;
import com.nexus.server.user.repository.UserRepository;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JdbcTemplate jdbc;

    private final String password =
            "Example-Secure-Passphrase-123!";

    private User createTestUser() {

        String email = "auth-" + UUID.randomUUID()
                + "@example.test";

        return users.create(
                email,
                "Test User",
                encoder.encode(password)
        );
    }

    @Test
    void authenticatesAgainstPostgreSQL() throws Exception {

        User user = createTestUser();

        MvcResult login = mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", user.email())
                                .param("password", password)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) login.getRequest()
                        .getSession(false);

        assertNotNull(session);

        // The session identifies the correct user.
        mvc.perform(
                        get("/api/v1/auth/me")
                                .session(session)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.displayName").value("Test User"));

        // Login alone does not grant infrastructure access.
        mvc.perform(
                        get("/api/v1/devices")
                                .session(session)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidCredentials() throws Exception {

        User user = createTestUser();

        mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", user.email())
                                .param("password", "WrongPassword")
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized());

        // Unknown accounts receive the same status.
        mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", "unknown@example.test")
                                .param("password", password)
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDisabledUsers() throws Exception {

        User user = createTestUser();

        jdbc.update(
                """
                UPDATE users
                SET disabled_at = NOW(),
                    updated_at = NOW()
                WHERE id = ?
                """,
                user.id()
        );

        mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", user.email())
                                .param("password", password)
                                .with(csrf())
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectsUnauthenticatedRequests() throws Exception {

        // The current-user endpoint requires login.
        mvc.perform(
                        get("/api/v1/auth/me")
                )
                .andExpect(status().isUnauthorized());

        // Login requires a CSRF token.
        mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", "test@example.test")
                                .param("password", "password")
                )
                .andExpect(status().isForbidden());

        // CSRF tokens can be requested publicly.
        mvc.perform(
                        get("/api/v1/auth/csrf")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").isString())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {

        User user = createTestUser();

        MvcResult login = mvc.perform(
                        post("/api/v1/auth/login")
                                .param("email", user.email())
                                .param("password", password)
                                .with(csrf())
                )
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) login.getRequest()
                        .getSession(false);

        assertNotNull(session);

        mvc.perform(
                        post("/api/v1/auth/logout")
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }
}
