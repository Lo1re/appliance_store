package com.epam.rd.autocode.assessment.appliances.security.password;

/** Committed security work uses only account metadata, never submitted passwords. */
public record PasswordChanged(Long accountId) { }
