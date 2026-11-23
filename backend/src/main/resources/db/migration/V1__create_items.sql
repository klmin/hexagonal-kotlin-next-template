CREATE TABLE items
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)             NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
