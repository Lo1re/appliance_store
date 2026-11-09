package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.exception.ApplianceException;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceInOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplianceServiceImplTest {
    @Mock private ApplianceRepository repository;
    @Mock private ApplianceInOrderRepository dependencies;
    @InjectMocks private ApplianceServiceImpl service;

    @Test
    void refusesDeletionWhileReferencedAndDoesNotCallDelete() {
        var entity = new Appliance();
        entity.setId(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(entity));
        when(dependencies.existsByAppliance_Id(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(ApplianceException.class)
                .hasMessage("appliance.in-use");
        verify(repository, never()).delete(entity);
    }
}
