package com.epam.rd.autocode.assessment.appliances.controller.password;
import com.epam.rd.autocode.assessment.appliances.dto.password.*;
import com.epam.rd.autocode.assessment.appliances.exception.password.*;
import com.epam.rd.autocode.assessment.appliances.security.password.reset.*;
import com.epam.rd.autocode.assessment.appliances.security.web.PasswordChangeLogoutHandler;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PasswordResetControllerBranchTest {
    final PasswordResetService passwords=mock(PasswordResetService.class);
    final PasswordResetController controller=new PasswordResetController(passwords,mock(PasswordChangeLogoutHandler.class));
    final MockHttpServletRequest request=new MockHttpServletRequest(); final MockHttpServletResponse response=new MockHttpServletResponse();
    final ExtendedModelMap model=new ExtendedModelMap(); final RedirectAttributesModelMap redirects=new RedirectAttributesModelMap();
    @Test void oversizedQueryTokenIsNotReflected() {
        assertThat(controller.reset("x".repeat(101),model)).isEqualTo("password/reset");
        assertThat(((PasswordResetRequest)model.get("resetForm")).getToken()).isEmpty();
    }
    @Test void requestStorageFailureReturnsSanitizedException() {
        var form=new ForgotPasswordRequest(); form.setEmail("user@example.test");
        when(passwords.request(any(),any(),any())).thenThrow(new DataAccessResourceFailureException("private"));
        assertThatThrownBy(()->controller.request(form,new BeanPropertyBindingResult(form,"forgotForm"),request,response,Locale.ENGLISH,model,redirects)).isInstanceOf(AuthenticationServiceException.class);
    }
    @Test void resetRejectsBindingErrorsBeforeService() {
        var form=new PasswordResetRequest();var errors=new BeanPropertyBindingResult(form,"resetForm");errors.reject("invalid");
        assertThat(controller.reset(form,errors,request,response,redirects)).isEqualTo("password/reset");verifyNoInteractions(passwords);
    }
    @Test void resetStorageFailureReturnsSanitizedException() {
        var form=new PasswordResetRequest();doThrow(new DataAccessResourceFailureException("private")).when(passwords).reset(form);
        assertThatThrownBy(()->controller.reset(form,new BeanPropertyBindingResult(form,"resetForm"),request,response,redirects)).isInstanceOf(AuthenticationServiceException.class);
    }
    @Test void forgotPageAndInvalidEmailDoNotIssueRecovery() {
        assertThat(controller.forgot(model)).isEqualTo("password/forgot");var form=new ForgotPasswordRequest();var errors=new BeanPropertyBindingResult(form,"forgotForm");errors.reject("invalid");
        assertThat(controller.request(form,errors,request,response,Locale.ENGLISH,model,redirects)).isEqualTo("password/forgot");assertThat(response.getStatus()).isEqualTo(400);verifyNoInteractions(passwords);
    }

}
