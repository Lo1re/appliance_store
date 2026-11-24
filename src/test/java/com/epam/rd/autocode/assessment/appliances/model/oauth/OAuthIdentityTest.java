package com.epam.rd.autocode.assessment.appliances.model.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OAuthIdentityTest {
    @Test
    void googleIdentityKeepsProviderAndSubjectOutOfDiagnosticText() {
        var identity = new OAuthIdentity("private-provider-subject", 7L);
        assertThat(identity.getProvider()).isEqualTo("GOOGLE");
        assertThat(identity.getSubject()).isEqualTo("private-provider-subject");
        assertThat(identity.getUserId()).isEqualTo(7L);
        assertThat(identity.toString())
                .startsWith("OAuthIdentity[id=")
                .doesNotContain("private-provider-subject");
    }
}
