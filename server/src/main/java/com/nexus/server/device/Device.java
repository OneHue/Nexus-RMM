package com.nexus.server.device;

import java.time.Instant;
import java.util.UUID;

public record Device(
        UUID id,
        UUID workspaceId,
        String name,
        String hostname,
        String operatingSystem,
        Instant createdAt,
        Instant lastSeenAt
) {

}
