package com.epam.rd.autocode.assessment.appliances.security.logging;

import com.epam.rd.autocode.assessment.appliances.security.identity.AccountPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public final class SecurityLogContext {
    private SecurityLogContext() {
    }

    public static Long currentAccountId() {
        return accountId(SecurityContextHolder.getContext().getAuthentication());
    }

    public static Long accountId(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
                ? principal.getAccountId() : null;
    }

    public static String remoteAddress(Authentication authentication) {
        return authentication != null && authentication.getDetails() instanceof WebAuthenticationDetails details
                ? safeRemoteAddress(details.getRemoteAddress()) : "-";
    }

    public static String safeRemoteAddress(String address) {
        return address != null && address.matches("[0-9a-fA-F:.%_-]{1,64}") ? address : "-";
    }
}
