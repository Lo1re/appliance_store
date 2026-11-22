package com.epam.rd.autocode.assessment.appliances.service;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {

    Page<ClientResponse> findAll(String search, Pageable pageable);

    ClientResponse findById(Long id);

    ClientResponse create(
            @NotNull @Valid ClientRequest request,
            @NotNull @Valid PasswordRequest password
    );

    ClientResponse update(Long id, @NotNull @Valid ClientRequest request);

    void delete(Long id);
}
