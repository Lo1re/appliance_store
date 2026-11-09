package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import com.epam.rd.autocode.assessment.appliances.exception.ManufacturerException;
import com.epam.rd.autocode.assessment.appliances.mapper.ManufacturerMapper;
import com.epam.rd.autocode.assessment.appliances.model.Manufacturer;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;
import com.epam.rd.autocode.assessment.appliances.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManufacturerServiceImpl implements ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;
    private final ApplianceRepository applianceRepository;
    private final ManufacturerMapper manufacturerMapper;

    @Override
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
    public ManufacturerResponse create(ManufacturerRequest request) {
        String name = request.getName().strip();

        if (manufacturerRepository.existsByName(name)) {
            throw new ManufacturerException("manufacturer.name.duplicate");
        }

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(name);

        Manufacturer saved = manufacturerRepository.save(manufacturer);
        return manufacturerMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ManufacturerResponse update(Long id, ManufacturerRequest request) {
        Manufacturer manufacturer = requireManufacturer(id);
        String name = request.getName().strip();

        if (manufacturerRepository.existsByNameAndIdNot(name, id)) {
            throw new ManufacturerException("manufacturer.name.duplicate");
        }

        manufacturer.setName(name);
        return manufacturerMapper.toResponse(manufacturer);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Manufacturer manufacturer = requireManufacturer(id);

        if (applianceRepository.existsByManufacturer_Id(id)) {
            throw new ManufacturerException("manufacturer.in-use");
        }

        manufacturerRepository.delete(manufacturer);
    }

    private Manufacturer requireManufacturer(Long id) {
        return manufacturerRepository.findById(id)
                .orElseThrow(() -> new ManufacturerException("manufacturer.not-found"));
    }
}