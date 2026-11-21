package com.epam.rd.autocode.assessment.appliances.dto.auth;

import java.time.Instant;

/** Cookie mode returns safe status metadata, never a duplicate access-token value. */
public record AccessTokenMetadata(long expiresIn, Instant expiresAt) { }
