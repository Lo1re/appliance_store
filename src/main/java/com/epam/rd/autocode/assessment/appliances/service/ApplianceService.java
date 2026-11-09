package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceRequest;
import com.epam.rd.autocode.assessment.appliances.dto.appliance.ApplianceResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplianceService {

    Page<ApplianceResponse> findAll(String search, Pageable pageable);

    ApplianceResponse findById(Long id);

    ApplianceResponse create(@NotNull @Valid ApplianceRequest request);

    ApplianceResponse update(Long id, @NotNull @Valid ApplianceRequest request);

    void delete(Long id);
}