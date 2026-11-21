package com.epam.rd.autocode.assessment.appliances.security.password.reset;
import com.epam.rd.autocode.assessment.appliances.dto.password.PasswordResetRequest;
import com.epam.rd.autocode.assessment.appliances.mail.SecurityMailLinkFactory;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import com.epam.rd.autocode.assessment.appliances.repository.lock.AccountRowLock;
import com.epam.rd.autocode.assessment.appliances.repository.password.PasswordResetRepository;
import com.epam.rd.autocode.assessment.appliances.repository.projection.CredentialSnapshot;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordCredentialStatusService;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenVersionService;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class PasswordResetServiceBranchTest {
    @Mock UserRepository users;@Mock AccountRowLock accounts;@Mock PasswordResetRepository tokens;@Mock ResetRequestLimiter limits;@Mock PasswordEncoder encoder;@Mock RefreshTokenRepository refresh;@Mock LoginAttemptRepository attempts;@Mock ApplicationEventPublisher events;@Mock PasswordCredentialStatusService state;@Mock AccessTokenVersionService versions;
    PasswordResetService service;AutoCloseable mocks;final ResetTokenCodec codec=new ResetTokenCodec();final Client client=new Client(1L,"Client","user@example.test","hash",null);
    @BeforeEach void setup(){mocks=MockitoAnnotations.openMocks(this);service=new PasswordResetService(users,accounts,tokens,codec,limits,encoder,refresh,attempts,events,Clock.systemUTC(),state,versions,new SecurityMailLinkFactory("http://localhost:8080"));}
    @AfterEach void close() throws Exception {mocks.close();}
    @Test void accountDisappearingDuringRequestDoesNotSendMail() {when(users.findByEmailIgnoreCase(client.getEmail())).thenReturn(Optional.of(client));assertThat(service.request(client.getEmail(),"source",Locale.ENGLISH)).isZero();verifyNoInteractions(events);}
    @Test void absentOrChangedCredentialSnapshotDoesNotSendMail() {
        when(users.findByEmailIgnoreCase(client.getEmail())).thenReturn(Optional.of(client));when(accounts.lock(1L)).thenReturn(Optional.of(client));
        assertThat(service.request(client.getEmail(),"source",Locale.ENGLISH)).isZero();
        when(users.findCredentialSnapshot(1L)).thenReturn(Optional.of(new CredentialSnapshot("changed@example.test","hash")));
        assertThat(service.request(client.getEmail(),"source",Locale.ENGLISH)).isZero();verifyNoInteractions(events);
    }
    @Test void expiredOrAlreadyConsumedTokenCannotChangePassword() {
        var form=form();when(tokens.owner(codec.hash(form.getToken()))).thenReturn(Optional.of(1L));when(accounts.lock(1L)).thenReturn(Optional.of(client));when(users.findCredentialSnapshot(1L)).thenReturn(Optional.of(new CredentialSnapshot(client.getEmail(),"hash")));
        assertThatThrownBy(()->service.reset(form)).isInstanceOf(InvalidPasswordResetException.class);verify(users,never()).replacePassword(any(),any(),any());
    }
    @Test void failedPasswordCompareAndSetCannotReportSuccess() {
        var form=form();when(tokens.owner(codec.hash(form.getToken()))).thenReturn(Optional.of(1L));when(accounts.lock(1L)).thenReturn(Optional.of(client));when(users.findCredentialSnapshot(1L)).thenReturn(Optional.of(new CredentialSnapshot(client.getEmail(),"hash")));when(tokens.consume(eq(1L),any(),any())).thenReturn(true);
        assertThatThrownBy(()->service.reset(form)).isInstanceOf(InvalidPasswordResetException.class);verifyNoInteractions(events);
    }
    PasswordResetRequest form(){var form=new PasswordResetRequest();form.setToken(codec.generate());form.setNewPassword("NewPassword2026!");form.setConfirmPassword("NewPassword2026!");return form;}
    @Test void malformedTokenNeverReadsAccountsOrCredentials() {
        var form=form();form.setToken("invalid");assertThatThrownBy(()->service.reset(form)).isInstanceOf(InvalidPasswordResetException.class);verifyNoInteractions(tokens,users,accounts);
    }

}
