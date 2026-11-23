package com.nexus.server.security;


import com.nexus.server.user.User;
import com.nexus.server.user.UserService;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserService users;

    public CurrentUserService(UserService users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User requireCurrentUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Access Denied: No authenticated user");
        }

        return users.findByEmail(authentication.getName())
                .filter(user -> !user.isDisabled())
                .orElseThrow(() ->
                        new AccessDeniedException("Access denied")
                );

    }

}
