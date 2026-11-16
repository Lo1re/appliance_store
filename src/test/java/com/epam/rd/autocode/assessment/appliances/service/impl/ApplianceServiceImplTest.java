package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrderRowRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplianceServiceImplTest {
    @Mock private BusinessAudit audit;

    @Mock private ApplianceRepository repository;
    @Mock private OrderRowRepository dependencies;
    @InjectMocks private ApplianceServiceImpl service;

    @Test
    void refusesDeletionWhileReferencedAndDoesNotCallDelete() {
        var entity = new Appliance();
        entity.setId(42L);

        when(repository.findById(42L)).thenReturn(Optional.of(entity));
        when(dependencies.existsByAppliance_Id(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(CatalogException.class)
                .hasMessage("appliance.in-use");

        verify(repository, never()).delete(entity);
    }
}
