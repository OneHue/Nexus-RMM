package com.nexus.server.workspace;

import java.time.Instant;
import java.util.UUID;

public record Workspace(
        UUID id,
        String name,
        String slug,

        // Instant is a moment in time
        Instant createdAt
) {

}
