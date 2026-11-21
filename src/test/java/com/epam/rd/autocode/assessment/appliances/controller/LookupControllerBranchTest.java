package com.epam.rd.autocode.assessment.appliances.controller;
import com.epam.rd.autocode.assessment.appliances.dto.lookup.SelectOption;
import com.epam.rd.autocode.assessment.appliances.service.lookup.LookupService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class LookupControllerBranchTest {
    @Test void delegatesEveryLookupWithoutClientSuppliedSelectedIds() {
        var service=mock(LookupService.class);var controller=new LookupController(service);var options=List.of(new SelectOption(1L,"Option"));
        when(service.products("p",List.of())).thenReturn(options);when(service.clients("c",List.of())).thenReturn(options);when(service.manufacturers("m",List.of())).thenReturn(options);
        assertThat(controller.products("p")).isEqualTo(options);assertThat(controller.clients("c")).isEqualTo(options);assertThat(controller.manufacturers("m")).isEqualTo(options);
    }
}
