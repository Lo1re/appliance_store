CREATE TABLE account_security_state (
    user_id BIGINT PRIMARY KEY,
    credential_version BIGINT NOT NULL,
    CONSTRAINT fk_account_security_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_account_credential_version CHECK (credential_version >= 0)
);
