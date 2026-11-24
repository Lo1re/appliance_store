package com.epam.rd.autocode.assessment.appliances.security.jwt.cookie;

import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.web.csrf.CsrfFilter;

// Restores unsafe-method CSRF checks after Resource Server's bearer exemption is applied.
public final class CookieCsrfProtection implements ObjectPostProcessor<CsrfFilter> {
    @Override
    public <O extends CsrfFilter> O postProcess(O filter) {
        filter.setRequireCsrfProtectionMatcher(CsrfFilter.DEFAULT_CSRF_MATCHER);
        return filter;
    }
}
