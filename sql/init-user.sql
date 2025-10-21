CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
                       user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       username VARCHAR(20) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_login_at TIMESTAMP,
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       is_online BOOLEAN NOT NULL DEFAULT FALSE,
                       CONSTRAINT username_length CHECK (char_length(username) >= 4)
);

CREATE INDEX idx_users_username ON users(username);