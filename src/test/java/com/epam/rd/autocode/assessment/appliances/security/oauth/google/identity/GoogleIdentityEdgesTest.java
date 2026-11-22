package com.epam.rd.autocode.assessment.appliances.security.oauth.google.identity;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.model.User;
import java.net.URL;import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GoogleIdentityEdgesTest {
 OidcUser user() throws Exception {var u=mock(OidcUser.class);when(u.getIssuer()).thenReturn(new URL("https://accounts.google.com"));when(u.getSubject()).thenReturn("sub");when(u.getEmail()).thenReturn("user@example.test");return u;}
 @Test void issuerSubjectAndEmailCannotCreateAnInvalidIdentity() throws Exception {
  var verifier=new GoogleIdentityVerifier();assertThatThrownBy(()->verifier.verify(null)).isInstanceOf(GoogleLoginException.class);
  for(String subject:new String[]{null," ","x".repeat(256)}){var u=user();when(u.getSubject()).thenReturn(subject);assertThatThrownBy(()->verifier.verify(u)).isInstanceOf(GoogleLoginException.class);}
  var absentIssuer=user();when(absentIssuer.getIssuer()).thenReturn(null);assertThatThrownBy(()->verifier.verify(absentIssuer)).isInstanceOf(GoogleLoginException.class);
  var untrusted=user();when(untrusted.getIssuer()).thenReturn(new URL("https://untrusted.example"));var invalid=untrusted;assertThatThrownBy(()->verifier.verify(invalid)).isInstanceOf(GoogleLoginException.class);
  for(String email:new String[]{null,"x".repeat(255),"invalid-email"}){var bad=user();when(bad.getEmail()).thenReturn(email);assertThat(verifier.verify(bad).email()).isEmpty();assertThat(verifier.verify(bad).emailVerified()).isFalse();}
  var good=user();when(good.getFullName()).thenReturn("x".repeat(110));assertThat(verifier.verify(good).name()).hasSize(100);
 }
 @Test void disappearedAndUnsupportedAccountsCannotBeAuthenticated(){var users=mock(UserRepository.class);var auth=new GoogleAccountAuthentication(users);assertThatThrownBy(()->auth.forAccount(1L)).isInstanceOf(GoogleLoginException.class);when(users.findById(1L)).thenReturn(Optional.of(new User()));assertThatThrownBy(()->auth.forAccount(1L)).isInstanceOf(GoogleLoginException.class);}
}
