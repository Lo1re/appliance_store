package com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity;

import com.epam.rd.autocode.assessment.appliances.validation.EmailAddress;
import java.util.Set;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/** Additional identity checks after the framework has verified signature, issuer, audience and nonce. */
@Component
public class GoogleIdentityVerifier {
    public VerifiedGoogleIdentity verify(OidcUser user) {
        if (user == null || user.getIssuer() == null
                || !Set.of("https://accounts.google.com", "accounts.google.com").contains(user.getIssuer().toString())
                || user.getSubject() == null || user.getSubject().isBlank() || user.getSubject().length() > 255) throw invalid();
        String email = EmailAddress.normalize(user.getEmail());
        boolean verifiedEmail = Boolean.TRUE.equals(user.getEmailVerified());
        if (email == null || email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+")) { email = ""; verifiedEmail = false; }
        String name = user.getFullName();
        return new VerifiedGoogleIdentity(user.getSubject(), email, name == null ? "" : name.substring(0, Math.min(100, name.length())), verifiedEmail);
    }
    private GoogleLoginException invalid() { return new GoogleLoginException(GoogleLoginException.Reason.INVALID_IDENTITY); }
}
