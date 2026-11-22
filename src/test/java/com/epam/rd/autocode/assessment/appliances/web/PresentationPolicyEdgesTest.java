package com.epam.rd.autocode.assessment.appliances.web;
import com.epam.rd.autocode.assessment.appliances.dto.order.*;
import com.epam.rd.autocode.assessment.appliances.web.error.MvcErrorViews;
import com.epam.rd.autocode.assessment.appliances.web.pagination.ListPageFactory;
import com.epam.rd.autocode.assessment.appliances.config.transport.HttpsRedirectConfiguration;
import com.epam.rd.autocode.assessment.appliances.web.transport.HttpsRedirectFilter;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import static org.assertj.core.api.Assertions.*;
class PresentationPolicyEdgesTest {
 @Test void approvedOrdersCannotBeApprovedAgain(){var summary=new OrderSummary(1L,2L,"Client",3L,"Employee",true);var caps=OrderCapabilities.forOrder(new OrderResponse(summary,java.util.List.of(),java.math.BigDecimal.ZERO),true);assertThat(caps.canApprove()).isFalse();}
 @Test void unknownClientStatusKeepsItsActualCode(){var view=MvcErrorViews.render(HttpStatusCode.valueOf(418));assertThat(view.getViewName()).isEqualTo("error/4xx");assertThat(view.getModel().get("status")).isEqualTo(418);}
 @Test void invalidTransportPortsAndFragmentsAreRejected(){for(int port:new int[]{-1,65536})assertThatThrownBy(()->new HttpsRedirectConfiguration().httpRedirectConnector(port)).isInstanceOf(IllegalArgumentException.class);assertThatThrownBy(()->new HttpsRedirectFilter("https://localhost#fragment")).isInstanceOf(IllegalArgumentException.class);assertThat(new ListPageFactory(10).create(0,10,"name,desc",Set.of("name"),"name")).isNotNull();}
}
