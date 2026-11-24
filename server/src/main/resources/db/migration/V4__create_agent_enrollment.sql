CREATE TABLE agent_enrollment_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    workspace_id UUID NOT NULL
        REFERENCES workspaces(id),

    created_by_user_id UUID NOT NULL
        REFERENCES users(id),

    device_name VARCHAR(120) NOT NULL,

    code_hash BYTEA NOT NULL UNIQUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
); -- Stores invitations created by administrators

CREATE INDEX idx_agent_enrollments_workspace
    ON agent_enrollment_codes(workspace_id);

CREATE TABLE agent_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    device_id UUID NOT NULL
        REFERENCES devices(id),

    secret_hash BYTEA NOT NULL UNIQUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ -- Allows for disabling the agent credential without losing the data.
); -- Stores credentials associated with registered devices

CREATE INDEX idx_agent_credentials_device
    ON agent_credentials(device_id);