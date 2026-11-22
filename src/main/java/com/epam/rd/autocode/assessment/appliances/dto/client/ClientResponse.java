package com.epam.rd.autocode.assessment.appliances.dto.client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientResponse {

    private Long id;
    private String name;
    private String email;
    private String card;
}
