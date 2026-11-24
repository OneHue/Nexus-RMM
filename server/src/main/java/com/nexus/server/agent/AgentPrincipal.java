package com.nexus.server.agent;

import java.util.UUID;

public record AgentPrincipal(
        UUID deviceId,
        UUID workspaceId
) {
}
