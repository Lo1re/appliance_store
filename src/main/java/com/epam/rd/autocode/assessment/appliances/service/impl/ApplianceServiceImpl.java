package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.persistence.PersistenceBoundary;
import com.epam.rd.autocode.assessment.appliances.persistence.PersistenceOperation;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAction;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.mapper.ApplianceMapper;
import com.epam.rd.autocode.assessment.appliances.model.Appliance;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrderRowRepository;
import com.epam.rd.autocode.assessment.appliances.service.ApplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@PreAuthorize("hasRole('EMPLOYEE')")
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplianceServiceImpl implements ApplianceService {

    private final ApplianceRepository applianceRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final OrderRowRepository applianceInOrderRepository;
    private final ApplianceMapper applianceMapper;

    private final BusinessAudit audit;

    @Override
    @PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
    public Page<ApplianceResponse> findAll(String search, Pageable pageable) {
        String query = search == null ? "" : search.strip();

        return applianceRepository.search(query, pageable)
                .map(applianceMapper::toResponse);
    }

    @Override
    public ApplianceResponse findById(Long id) {
        return applianceMapper.toResponse(requireAppliance(id));
    }

    @Override
    @Transactional
    @Loggable
    @PersistenceBoundary(PersistenceOperation.APPLIANCE_WRITE)
    public ApplianceResponse create(ApplianceRequest request) {
        Manufacturer manufacturer = requireManufacturer(request.getManufacturerId());

        Appliance appliance = new Appliance();
        applianceMapper.applyRequest(request, appliance);
        appliance.setManufacturer(manufacturer);

        Appliance saved = applianceRepository.save(appliance);
        audit.record(BusinessAction.APPLIANCE_CREATED, saved.getId());
        return applianceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Loggable
    @PersistenceBoundary(PersistenceOperation.APPLIANCE_WRITE)
    public ApplianceResponse update(Long id, ApplianceRequest request) {
        Appliance appliance = requireAppliance(id);
        Manufacturer manufacturer = requireManufacturer(request.getManufacturerId());

        applianceMapper.applyRequest(request, appliance);
        appliance.setManufacturer(manufacturer);
        audit.record(BusinessAction.APPLIANCE_UPDATED, appliance.getId());
        return applianceMapper.toResponse(appliance);
    }

    @Override
    @Transactional
    @Loggable
    @PersistenceBoundary(PersistenceOperation.APPLIANCE_DELETE)
    public void delete(Long id) {
        Appliance appliance = requireAppliance(id);

        if (applianceInOrderRepository.existsByAppliance_Id(id)) {
            throw new CatalogException(CatalogError.APPLIANCE_IN_USE);
        }

        applianceRepository.delete(appliance);
        audit.record(BusinessAction.APPLIANCE_DELETED, appliance.getId());
    }

    private Appliance requireAppliance(Long id) {
        return applianceRepository.findById(id)
                .orElseThrow(() -> new CatalogException(CatalogError.APPLIANCE_NOT_FOUND));
    }

    private Manufacturer requireManufacturer(Long id) {
        return manufacturerRepository.findById(id)
                .orElseThrow(() -> new CatalogException(
                        CatalogError.APPLIANCE_MANUFACTURER_NOT_FOUND
                ));
    }
}
