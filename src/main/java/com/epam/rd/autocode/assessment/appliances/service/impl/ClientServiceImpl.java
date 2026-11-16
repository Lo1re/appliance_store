package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.aspect.Loggable;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAction;
import com.epam.rd.autocode.assessment.appliances.logging.BusinessAudit;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.mapper.ClientMapper;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.OrdersRepository;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
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
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final OrdersRepository ordersRepository;
    private final ClientMapper clientMapper;
    private final AccountCredentialsService credentials;

    private final BusinessAudit audit;

    @Override
    public Page<ClientResponse> findAll(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return clientRepository.findAll(pageable).map(clientMapper::toResponse);
        }

        String query = search.strip();
        return clientRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, pageable)
                .map(clientMapper::toResponse);
    }

    @Override
    public ClientResponse findById(Long id) {
        return clientMapper.toResponse(requireClient(id));
    }

    @Override
    @Transactional
    @Loggable
    public ClientResponse create(ClientRequest request, PasswordRequest password) {
        var client = new Client();
        clientMapper.applyRequest(request, client);
        credentials.assignEmail(client, request.getEmail());
        credentials.setPassword(client, password.getPassword());

        Client saved = clientRepository.save(client);
        audit.record(BusinessAction.CLIENT_CREATED, saved.getId());
        return clientMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Loggable
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = requireClient(id);
        clientMapper.applyRequest(request, client);
        credentials.assignEmail(client, request.getEmail());
        audit.record(BusinessAction.CLIENT_UPDATED, client.getId());
        return clientMapper.toResponse(client);
    }

    @Override
    @Transactional
    @Loggable
    public void delete(Long id) {
        Client client = requireClient(id);

        if (ordersRepository.existsByClient_Id(id)) {
            throw new AccountException(AccountError.CLIENT_IN_USE);
        }

        clientRepository.delete(client);
        audit.record(BusinessAction.CLIENT_DELETED, client.getId());
    }

    private Client requireClient(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new AccountException(AccountError.CLIENT_NOT_FOUND));
    }
}
