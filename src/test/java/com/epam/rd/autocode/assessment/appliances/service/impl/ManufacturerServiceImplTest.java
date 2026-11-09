package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.exception.ManufacturerException;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
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
class ManufacturerServiceImplTest {
    @Mock private ManufacturerRepository repository;
    @Mock private ApplianceRepository dependencies;
    @InjectMocks private ManufacturerServiceImpl service;

    @Test
    void refusesDeletionWhileReferencedAndDoesNotCallDelete() {
        var entity = new Manufacturer();
        entity.setId(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(entity));
        when(dependencies.existsByManufacturer_Id(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(ManufacturerException.class)
                .hasMessage("manufacturer.in-use");
        verify(repository, never()).delete(entity);
    }
}
