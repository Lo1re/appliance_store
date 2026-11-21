package com.epam.rd.autocode.assessment.appliances.service.lookup;
import com.epam.rd.autocode.assessment.appliances.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class LookupServiceBranchTest {
    @Test void ignoresMissingNonPositiveAndDuplicateSelectedIdsAndNormalizesNullQuery() {
        var products=mock(ApplianceRepository.class);var clients=mock(ClientRepository.class);var manufacturers=mock(ManufacturerRepository.class);
        var service=new LookupService(products,clients,manufacturers);
        when(products.search(any(),any())).thenReturn(Page.empty());when(clients.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(any(),any(),any())).thenReturn(Page.empty());when(manufacturers.findByNameContainingIgnoreCase(any(),any())).thenReturn(Page.empty());
        assertThat(service.products(null,null)).isEmpty();
        var selected=new com.epam.rd.autocode.assessment.appliances.model.Client(2L,"Selected","selected@example.test","hash",null);
        when(clients.findAllById(List.of(2L))).thenReturn(List.of(selected));
        assertThat(service.clients("  query  ",Arrays.asList(null,-1L,0L,2L,2L))).extracting(option->option.id()).containsExactly(2L);service.manufacturers(" ",List.of());
        verify(products).search(eq(""),any());verify(clients).findAllById(List.of(2L));verify(clients).findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(eq("query"),eq("query"),any());
    }
}
