package com.epam.rd.autocode.assessment.appliances.security.identity;

/** Stable local identity shared by authentication adapters; never a JPA entity. */
public interface AccountPrincipal {
    Long getAccountId();
}
