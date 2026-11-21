CREATE TABLE password_credential_state (
    user_id BIGINT PRIMARY KEY,
    password_set BOOLEAN NOT NULL,
    CONSTRAINT fk_password_credential_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
