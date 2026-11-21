CREATE TABLE pending_registration (
    token_hash VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    card VARCHAR(32),
    password_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_pending_registration_email CHECK (email = lower(trim(email)))
);
CREATE INDEX ix_pending_registration_expiry ON pending_registration (expires_at);
