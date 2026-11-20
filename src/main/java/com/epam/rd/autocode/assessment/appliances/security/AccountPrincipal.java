package com.epam.rd.autocode.assessment.appliances.security;

/** Stable local identity shared by authentication adapters; never a JPA entity. */
public interface AccountPrincipal {
    Long getAccountId();
}
