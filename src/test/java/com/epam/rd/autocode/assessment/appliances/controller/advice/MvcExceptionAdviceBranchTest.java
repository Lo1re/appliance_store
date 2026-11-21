package com.epam.rd.autocode.assessment.appliances.controller.advice;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import static org.assertj.core.api.Assertions.*;
class MvcExceptionAdviceBranchTest {
    @Test void authenticationStorageOutageIs503WithoutPrivateMessage() {var result=new MvcExceptionAdvice().handleFailure(new AuthenticationServiceException("private"),new MockHttpServletResponse());assertThat(result.getStatus().value()).isEqualTo(503);assertThat(result.getModel().toString()).doesNotContain("private");}
}
