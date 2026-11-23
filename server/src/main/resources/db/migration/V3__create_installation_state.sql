CREATE TABLE nexus_installation (
    id SMALLINT PRIMARY KEY,
    initialized_at TIMESTAMPTZ,

    CONSTRAINT chk_single_installation
        CHECK (id = 1)
);

INSERT INTO nexus_installation (id, initialized_at)
VALUES (1, NULL);