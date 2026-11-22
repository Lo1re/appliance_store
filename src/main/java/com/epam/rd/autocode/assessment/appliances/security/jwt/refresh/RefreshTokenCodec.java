package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;

import com.epam.rd.autocode.assessment.appliances.security.jwt.config.ConditionalOnJwt;
import com.epam.rd.autocode.assessment.appliances.security.token.OpaqueTokens;
import org.springframework.stereotype.Component;

/** Refresh boundary rejects noncanonical input before looking up its storage fingerprint. */
@Component
@ConditionalOnJwt
public class RefreshTokenCodec {
    public String generate() {
        return OpaqueTokens.generate();
    }

    public String hash(String token) {
        if (!OpaqueTokens.isCanonical(token)) {
            throw new InvalidRefreshTokenException();
        }
        return OpaqueTokens.fingerprint(token);
    }
}
