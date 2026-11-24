package com.epam.rd.autocode.assessment.appliances.service.lookup;

import com.epam.rd.autocode.assessment.appliances.dto.lookup.SelectOption;
import com.epam.rd.autocode.assessment.appliances.repository.ApplianceRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ClientRepository;
import com.epam.rd.autocode.assessment.appliances.repository.ManufacturerRepository;

import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

@Validated
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('CLIENT', 'EMPLOYEE')")
public class LookupService {
    private static final int RESULT_LIMIT = 20;
    private final ApplianceRepository appliances;
    private final ClientRepository clients;
    private final ManufacturerRepository manufacturers;

    public List<SelectOption> products(
            @Size(max = 100) String query, @Size(max = 50) Collection<Long> selectedIds) {
        var selected = ids(selectedIds);
        return merge(
                appliances.search(query(query), page()).getContent().stream()
                        .map(
                                p ->
                                        new SelectOption(
                                                p.getId(), p.getName() + " (" + p.getModel() + ")"))
                        .toList(),
                () ->
                        appliances.findAllById(selected).stream()
                                .map(
                                        p ->
                                                new SelectOption(
                                                        p.getId(),
                                                        p.getName() + " (" + p.getModel() + ")"))
                                .toList());
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<SelectOption> clients(
            @Size(max = 100) String query, @Size(max = 50) Collection<Long> selectedIds) {
        var selected = ids(selectedIds);
        String search = query(query);
        return merge(
                clients
                        .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                                search, search, page())
                        .getContent()
                        .stream()
                        .map(c -> new SelectOption(c.getId(), c.getName() + " — " + c.getEmail()))
                        .toList(),
                () ->
                        clients.findAllById(selected).stream()
                                .map(
                                        c ->
                                                new SelectOption(
                                                        c.getId(),
                                                        c.getName() + " — " + c.getEmail()))
                                .toList());
    }

    public List<SelectOption> manufacturers(
            @Size(max = 100) String query, @Size(max = 50) Collection<Long> selectedIds) {
        var selected = ids(selectedIds);
        return merge(
                manufacturers
                        .findByNameContainingIgnoreCase(query(query), page())
                        .getContent()
                        .stream()
                        .map(m -> new SelectOption(m.getId(), m.getName()))
                        .toList(),
                () ->
                        manufacturers.findAllById(selected).stream()
                                .map(m -> new SelectOption(m.getId(), m.getName()))
                                .toList());
    }

    private PageRequest page() {
        return PageRequest.of(0, RESULT_LIMIT, Sort.by("name", "id"));
    }

    private String query(String value) {
        return value == null ? "" : value.strip();
    }

    private List<Long> ids(Collection<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().filter(id -> id != null && id > 0).distinct().toList();
    }

    private List<SelectOption> merge(
            List<SelectOption> page, Supplier<List<SelectOption>> selected) {
        var options = new LinkedHashMap<Long, SelectOption>();
        page.forEach(option -> options.put(option.id(), option));
        selected.get().forEach(option -> options.putIfAbsent(option.id(), option));
        return List.copyOf(options.values());
    }
}
