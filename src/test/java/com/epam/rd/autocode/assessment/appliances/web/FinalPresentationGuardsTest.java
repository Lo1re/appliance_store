package com.epam.rd.autocode.assessment.appliances.web;
import com.epam.rd.autocode.assessment.appliances.dto.order.*;
import com.epam.rd.autocode.assessment.appliances.web.form.*;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;
import com.epam.rd.autocode.assessment.appliances.web.transport.HttpsRedirectFilter;
import com.epam.rd.autocode.assessment.appliances.service.*;
import com.epam.rd.autocode.assessment.appliances.service.lookup.LookupService;
import com.epam.rd.autocode.assessment.appliances.mapper.OrderMapper;
import com.epam.rd.autocode.assessment.appliances.security.identity.CurrentActor;
import java.math.BigDecimal;import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class FinalPresentationGuardsTest {
 @Test void capabilityMatrixCoversClientsEmployeesAndBothOrderStates(){for(boolean approved:new boolean[]{true,false})for(boolean employee:new boolean[]{true,false}){var summary=new OrderSummary(1L,2L,"Client",3L,"Employee",approved);var c=OrderCapabilities.forOrder(new OrderResponse(summary,List.of(),BigDecimal.ZERO),employee);assertThat(c.canEdit()).isEqualTo(!approved);assertThat(c.canDelete()).isEqualTo(!approved);assertThat(c.canApprove()).isEqualTo(employee&&!approved);}}
 @Test void redisplayingAnExistingOrderRechecksEditability(){var orders=mock(OrderService.class);var forms=new OrderFormAssembler(orders,mock(OrderMapper.class),mock(LookupService.class),mock(CurrentActor.class));assertThat(forms.redisplay(1L,null,new OrderRequest()).editing()).isTrue();verify(orders).requireEditable(1L);}
 @Test void upperPaginationLimitAndEveryInvalidOriginComponentAreRejected(){assertThatThrownBy(()->new ListPageFactory(10).create(0,0,"id",Set.of("id"),"id")).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->new ListPageFactory(10).create(0,101,"id",Set.of("id"),"id")).isInstanceOf(IllegalArgumentException.class);for(String uri:new String[]{"http://localhost","https:/relative","https://user@localhost","https://localhost?q=1","https://localhost#fragment","https://localhost/path"})assertThatThrownBy(()->new HttpsRedirectFilter(uri)).as(uri).isInstanceOf(IllegalArgumentException.class);}
}

