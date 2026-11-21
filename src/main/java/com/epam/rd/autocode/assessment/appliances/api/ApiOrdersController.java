package com.epam.rd.autocode.assessment.appliances.api;

import com.epam.rd.autocode.assessment.appliances.dto.order.ApiOrderCreateRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderRequest;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderResponse;
import com.epam.rd.autocode.assessment.appliances.dto.order.OrderSummary;
import com.epam.rd.autocode.assessment.appliances.dto.query.ApiListQuery;
import com.epam.rd.autocode.assessment.appliances.dto.query.PageResponse;
import com.epam.rd.autocode.assessment.appliances.service.OrderService;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.api.enabled", havingValue = "true")
public class ApiOrdersController {
    private static final Set<String> SORT_FIELDS = Set.of("id", "approved");
    private final OrderService orders;
    private final ListPageFactory pages;

    @GetMapping public PageResponse<OrderSummary> list(@Valid @ModelAttribute ApiListQuery query,
                                                      @RequestParam(required = false) Boolean approved) {
        var page = pages.create(query.getPage(), query.getSize(), query.getSort(), SORT_FIELDS, "id");
        return PageResponse.from(orders.findAll(approved, page.pageable()));
    }
    @GetMapping("/{id}") public OrderResponse details(@PathVariable Long id) { return orders.findById(id); }
    @PostMapping public ResponseEntity<OrderResponse> create(@Valid @RequestBody ApiOrderCreateRequest request) {
        var order = orders.create(request.clientId(), request.order());
        return ResponseEntity.created(URI.create("/api/orders/" + order.summary().id())).body(order);
    }
    @PutMapping("/{id}") public OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return orders.update(id, request);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) {
        orders.delete(id); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/approve") public OrderResponse approve(@PathVariable Long id) { return orders.approve(id); }
}
