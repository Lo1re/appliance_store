package com.epam.rd.autocode.assessment.appliances.security.registration;
import com.epam.rd.autocode.assessment.appliances.exception.*;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import com.epam.rd.autocode.assessment.appliances.security.password.PasswordCredentialStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class ClientRegistrationServiceBranchTest {
    final UserRepository users=mock(UserRepository.class);final ClientRepository clients=mock(ClientRepository.class);
    final ClientRegistrationService service=new ClientRegistrationService(users,clients,mock(PasswordCredentialStatusService.class),mock(BusinessAudit.class),mock(ApplicationEventPublisher.class));
    final RegistrationDetails details=new RegistrationDetails("Client","client@example.test","CARD","hash");
    @Test void rechecksEmailAtConfirmation() {when(users.existsByEmailIgnoreCase(details.email())).thenReturn(true);assertThatThrownBy(()->service.register(details)).isInstanceOf(AccountException.class).hasMessage("user.email.duplicate");verifyNoInteractions(clients);}
    @Test void rechecksCardAtConfirmation() {when(clients.existsByCard("CARD")).thenReturn(true);assertThatThrownBy(()->service.register(details)).isInstanceOf(AccountException.class).hasMessage("client.card.duplicate");verify(clients,never()).saveAndFlush(any());}
    @Test void availableCardCreatesClientAndRedactsInternalData() {when(clients.saveAndFlush(any())).thenReturn(new Client(1L,"Client",details.email(),"hash","CARD"));assertThat(service.register(details)).isEqualTo(1L);assertThat(details.toString()).doesNotContain("hash",details.email());assertThat(new RegistrationPreparation.Result(0,null).toString()).contains("redacted");}
    @Test void clientMayBeCreatedWithoutACard() {var details=new RegistrationDetails("Client","client@example.test",null,"hash");when(clients.saveAndFlush(any())).thenReturn(new Client(1L,"Client",details.email(),"hash",null));assertThat(service.register(details)).isEqualTo(1L);verify(clients,never()).existsByCard(any());}
}
