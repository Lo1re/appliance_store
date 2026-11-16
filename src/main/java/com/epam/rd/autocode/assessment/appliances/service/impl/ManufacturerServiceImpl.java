package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.mapper.ManufacturerMapper;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
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
public class ManufacturerServiceImpl implements ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;
    private final ApplianceRepository applianceRepository;
    private final ManufacturerMapper manufacturerMapper;

    @Override
    @PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
    public Page<ManufacturerResponse> findAll(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return manufacturerRepository.findAll(pageable)
                    .map(manufacturerMapper::toResponse);
        }

        return manufacturerRepository
                .findByNameContainingIgnoreCase(search.strip(), pageable)
                .map(manufacturerMapper::toResponse);
    }

    @Override
    public ManufacturerResponse findById(Long id) {
        return manufacturerMapper.toResponse(requireManufacturer(id));
    }

    @Override
    @Transactional
    @Loggable
    public ManufacturerResponse create(ManufacturerRequest request) {
        if (manufacturerRepository.existsByName(request.getName())) {
            throw new CatalogException(CatalogError.MANUFACTURER_DUPLICATE_NAME);
        }

        Manufacturer manufacturer = new Manufacturer();
        manufacturerMapper.applyRequest(request, manufacturer);

        Manufacturer saved = manufacturerRepository.save(manufacturer);
        return manufacturerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Loggable
    public ManufacturerResponse update(Long id, ManufacturerRequest request) {
        Manufacturer manufacturer = requireManufacturer(id);

        if (manufacturerRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new CatalogException(CatalogError.MANUFACTURER_DUPLICATE_NAME);
        }

        manufacturerMapper.applyRequest(request, manufacturer);
        return manufacturerMapper.toResponse(manufacturer);
    }

    @Override
    @Transactional
    @Loggable
    public void delete(Long id) {
        Manufacturer manufacturer = requireManufacturer(id);

        if (applianceRepository.existsByManufacturer_Id(id)) {
            throw new CatalogException(CatalogError.MANUFACTURER_IN_USE);
        }

        manufacturerRepository.delete(manufacturer);
    }

    private Manufacturer requireManufacturer(Long id) {
        return manufacturerRepository.findById(id)
                .orElseThrow(() -> new CatalogException(CatalogError.MANUFACTURER_NOT_FOUND));
    }
}
