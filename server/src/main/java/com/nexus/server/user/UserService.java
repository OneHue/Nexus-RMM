package com.nexus.server.user;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID is required"
            );
        }

        return repository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "User not found"
                ));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                 "Email is required"
            );
        };

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        if (normalizedEmail.length() > 254) {
            throw new IllegalArgumentException(
                    "Email cannot exceed 254 characters"
            );
        }

        return repository.findByEmail(normalizedEmail);
    }

    @Transactional(readOnly = true)
    public User requireActiveUser(UUID userId) {

        User user = getUser(userId);

        if (user.isDisabled()) {
            throw new SecurityException(
                    "User account is disabled"
            );
        }

        return user;

    }

}
