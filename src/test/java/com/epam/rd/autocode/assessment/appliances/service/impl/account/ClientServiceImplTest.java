package com.epam.rd.autocode.assessment.appliances.service.impl.account;

import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {
    @Mock private BusinessAudit audit;
    @Mock private AccountCredentialsService credentials;
    @Mock private ClientRepository repository;
    @Mock private OrdersRepository dependencies;
    @InjectMocks private ClientServiceImpl service;

    @Test
    void refusesDeletionWhileReferencedAndDoesNotCallDelete() {
        var entity = new Client();
        entity.setId(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(entity));
        when(dependencies.existsByClient_Id(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(AccountException.class)
                .hasMessage("client.in-use");
        verify(repository, never()).delete(entity);
    }
}
