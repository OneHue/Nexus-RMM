CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Ensures a device cant exist without a workspace
    workspace_id UUID NOT NULL,

    name VARCHAR(120) NOT NULL,
    hostname VARCHAR(255),
    operating_system VARCHAR(120),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ,

    CONSTRAINT fk_device_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id)
);

CREATE INDEX idx_devices_workspace_id
    ON devices(workspace_id);