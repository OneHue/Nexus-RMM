package com.nexus.server.user;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String displayName,
        Instant emailVerifiedAt,
        Instant disabledAt,
        Instant createdAt,
        Instant updatedAt
) {

    public boolean isDisabled() {
        return disabledAt != null;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

}
