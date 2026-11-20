package com.nexus.server.workspace;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMembership(
        UUID workspaceId,
        UUID userId,
        WorkspaceRole role,
        Instant createdAt
) {

}
