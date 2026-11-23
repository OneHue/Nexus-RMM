package com.nexus.server.security.controller;


import com.nexus.server.user.User;
import com.nexus.server.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    public record CsrfResponse(
            String headerName,
            String token
    ) { }

    public record CurrentUserResponse(
            UUID id,
            String email,
            String displayName
    ) { }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    // Spring Boot supplies authentication identity
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        User user = userService.findByEmail(authentication.getName()).orElse(null);

        if (user == null || user.isDisabled()) {
            new SecurityContextLogoutHandler().logout(
                    request,
                    response,
                    authentication
            );

            // If the user found by email is null or disabled, then send the HttpStatus UNAUTHORIZED
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        }

        // If the null and disabled check passes, then return a response entity that is "ok" with the new CurrentUserResponse containing id, email, and display name
        return ResponseEntity.ok(
                new CurrentUserResponse(
                        user.id(),
                        user.email(),
                        user.displayName()
                )
        );

    }

}
