-- users (UUID primary key)
CREATE TABLE users (
    uuid VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- refresh tokens (store hashed tokens)
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_uuid VARCHAR(36) NOT NULL,
    expiry TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_uuid) REFERENCES users(uuid) ON DELETE CASCADE
);

-- jti blacklist (token revocation)
CREATE TABLE jti_blacklist (
    jti VARCHAR(128) PRIMARY KEY,
    expiry TIMESTAMP NOT NULL
);