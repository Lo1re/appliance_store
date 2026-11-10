package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.dto.client.ClientResponse;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientMapper {

    private final ModelMapper modelMapper;

    public void applyRequest(ClientRequest request, Client client) {
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setCard(request.getCard());
    }

    public ClientResponse toResponse(Client client) {
        return modelMapper.map(client, ClientResponse.class);
    }

    public ClientRequest toRequest(ClientResponse response) {
        var request = new ClientRequest();
        request.setName(response.getName());
        request.setEmail(response.getEmail());
        request.setCard(response.getCard());
        return request;
    }
}