package com.epam.rd.autocode.assessment.appliances.dto.auth;
import com.epam.rd.autocode.assessment.appliances.dto.query.ApiListQuery;
import com.epam.rd.autocode.assessment.appliances.dto.oauth.GoogleProfileRequest;
import com.epam.rd.autocode.assessment.appliances.exception.password.PasswordChangeError;
import com.epam.rd.autocode.assessment.appliances.repository.projection.CredentialSnapshot;
import com.epam.rd.autocode.assessment.appliances.mail.*;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.PasswordResetMail;
import java.net.URI;import java.time.Instant;import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RedactedCredentialRepresentationsTest {
 @Test void diagnosticRepresentationsNeverRevealSecretsOrRecipients(){
  var link=URI.create("https://localhost/?token=private-secret");var refresh=new RefreshTokenRequest("private-secret");
  Object[] values={new AccessTokenResponse("private-secret","Bearer",15,Instant.EPOCH),new TokenPairResponse("private-secret","Bearer",15,Instant.EPOCH,"private-secret",Instant.EPOCH),new ApiCsrfTokenResponse("header","param","private-secret"),refresh,new CredentialSnapshot("private@example.test","private-secret"),new PasswordResetMail("private@example.test",link,Locale.ENGLISH),new EmailVerificationMail("private@example.test",link,Locale.ENGLISH),new RegistrationVerificationMail("private@example.test",link,Locale.ENGLISH),new SecurityNotificationMail("private@example.test",SecurityNotificationMail.Kind.PASSWORD_CHANGED,Locale.ENGLISH),new GoogleProfileRequest()};
  for(Object value:values)assertThat(value.toString()).doesNotContain("private-secret","private@example.test");
  assertThat(PasswordChangeError.CURRENT_INVALID.kind()).isNotNull();
 }
 @Test void queryAndProfileDtosPreserveTheirExplicitInputContract(){var query=new ApiListQuery();query.setSearch("query");query.setPage(2);query.setSize(5);query.setSort("id");assertThat(query.getSearch()).isEqualTo("query");assertThat(query.getPage()).isEqualTo(2);assertThat(query.getSize()).isEqualTo(5);assertThat(query.getSort()).isEqualTo("id");var profile=new GoogleProfileRequest();profile.setName(null);assertThat(profile.getName()).isNull();}
}

