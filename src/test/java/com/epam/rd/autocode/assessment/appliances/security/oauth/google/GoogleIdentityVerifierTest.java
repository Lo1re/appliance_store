package com.epam.rd.autocode.assessment.appliances.security.oauth.google;

import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleIdentityVerifier;
import com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity.GoogleLoginException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import static org.assertj.core.api.Assertions.*;

class GoogleIdentityVerifierTest {
    @Test void acceptsOnlyGoogleIssuerAndPreservesWhetherEmailWasVerified() {
        var verifier = new GoogleIdentityVerifier();
        var verified = verifier.verify(user("https://accounts.google.com", "USER@example.test", true));
        assertThat(verified.email()).isEqualTo("user@example.test");
        assertThat(verified.emailVerified()).isTrue();
        assertThat(verifier.verify(user("https://accounts.google.com", "user@example.test", false)).emailVerified()).isFalse();
        assertThatThrownBy(() -> verifier.verify(user("https://attacker.example", "user@example.test", true)))
                .isInstanceOf(GoogleLoginException.class);
        assertThat(verified.toString()).doesNotContain(verified.email(), verified.subject());
    }
    private DefaultOidcUser user(String issuer, String email, boolean verified) {
        var claims = new HashMap<String, Object>();
        claims.put("iss", issuer); claims.put("sub", "google-subject"); claims.put("email", email); claims.put("email_verified", verified);
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")),
                new OidcIdToken("not-a-real-provider-token", Instant.now(), Instant.now().plusSeconds(600), claims));
    }
}
