CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    email VARCHAR(254) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash TEXT NOT NULL,

    email_verified_at TIMESTAMPTZ,
    disabled_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_users_email_not_blank
        CHECK (TRIM(email) <> ''),

    CONSTRAINT chk_users_display_name_not_blank
        CHECK (TRIM(display_name) <> '')
);

CREATE UNIQUE INDEX idx_users_email_unique
    ON users (LOWER(email));

CREATE TABLE workspace_memberships (
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,

    role VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_workspace_memberships
        PRIMARY KEY (workspace_id, user_id),

    CONSTRAINT fk_membership_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES workspaces(id),

    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT chk_membership_role
        CHECK (
            role IN (
                'OWNER',
                'ADMIN',
                'OPERATOR',
                'VIEWER'
            )
        )
);

CREATE INDEX idx_workspace_membership_user_id
    ON workspace_memberships(user_id);