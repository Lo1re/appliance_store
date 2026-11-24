package com.epam.rd.autocode.assessment.appliances.security.password.reset;

import com.epam.rd.autocode.assessment.appliances.security.token.OpaqueTokens;

import org.springframework.stereotype.Component;

// Recovery-token boundary; hashing also supports rate-limit identifiers.
@Component
public class ResetTokenCodec {
    public String generate() {
        return OpaqueTokens.generate();
    }

    public boolean valid(String value) {
        return OpaqueTokens.isCanonical(value);
    }

    public String hash(String value) {
        return OpaqueTokens.fingerprint(value);
    }
}
