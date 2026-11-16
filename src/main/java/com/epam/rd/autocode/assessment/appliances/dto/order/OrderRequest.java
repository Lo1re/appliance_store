package com.epam.rd.autocode.assessment.appliances.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {

    @NotEmpty
    @Size(max = 50)
    private List<@NotNull @Valid OrderItemRequest> items;
}
