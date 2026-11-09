package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerRequest;
import com.epam.rd.autocode.assessment.appliances.dto.manufacturer.ManufacturerResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ManufacturerService {

    Page<ManufacturerResponse> findAll(String search, Pageable pageable);

    ManufacturerResponse findById(Long id);

    ManufacturerResponse create(@NotNull @Valid ManufacturerRequest request);

    ManufacturerResponse update(Long id, @NotNull @Valid ManufacturerRequest request);

    void delete(Long id);
}