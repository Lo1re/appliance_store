package com.epam.rd.autocode.assessment.appliances.mapper;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import static org.assertj.core.api.Assertions.assertThat;

class ClientMapperTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void keepsIdentityAndPasswordAndOmitsPasswordFromResponse() {
        var client = new Client(7L, "Old name", "old@example.com", "stored-hash", "OLD");
        var clientRequest = new ClientRequest();
        clientRequest.setName(" New name ");
        clientRequest.setEmail(" NEW@EXAMPLE.COM ");
        clientRequest.setCard(" NEW-CARD ");
        var clientMapper = Mappers.getMapper(ClientMapper.class);
        clientMapper.applyRequest(clientRequest, client);
        var clientResponse = clientMapper.toResponse(client);
        assertThat(clientResponse.getId()).isEqualTo(7L);
        assertThat(clientResponse.getName()).isEqualTo("New name");
        assertThat(clientResponse.getEmail()).isEqualTo("old@example.com");
        assertThat(clientResponse.getCard()).isEqualTo("NEW-CARD");
        assertThat(client.getPassword()).isEqualTo("stored-hash");
        assertThat(json.valueToTree(clientResponse).has("password")).isFalse();
    }
}
