CREATE TABLE login_attempt (
    email VARCHAR(254) NOT NULL,
    failed_count INTEGER NOT NULL,
    first_failed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_until TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_login_attempt PRIMARY KEY (email),
    CONSTRAINT ck_login_attempt_email CHECK (email = LOWER(TRIM(email))),
    CONSTRAINT ck_login_attempt_count CHECK (failed_count >= 0 AND failed_count <= 100)
);

CREATE INDEX ix_login_attempt_expiration ON login_attempt (first_failed_at);
