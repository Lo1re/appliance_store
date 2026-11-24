package com.epam.rd.autocode.assessment.appliances.service.impl.catalog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceImplTest {
    @Mock private BusinessAudit audit;

    @Mock private ManufacturerRepository repository;

    @Mock private ApplianceRepository applianceRepository;

    @InjectMocks private ManufacturerServiceImpl service;

    @Test
    void refusesDeletionWhileReferencedAndDoesNotCallDelete() {
        var manufacturer = new Manufacturer();
        manufacturer.setId(42L);

        when(repository.findById(42L)).thenReturn(Optional.of(manufacturer));
        when(applianceRepository.existsByManufacturer_Id(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(CatalogException.class)
                .hasMessage("manufacturer.in-use");

        verify(repository, never()).delete(manufacturer);
    }
}
