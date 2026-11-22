package com.epam.rd.autocode.assessment.appliances.validation.password;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class PasswordConfirmationEdgesTest {
 record Form(String newPassword,String confirmPassword) implements PasswordConfirmation {public String getNewPassword(){return newPassword;}public String getConfirmPassword(){return confirmPassword;}}
 @Test void absentFieldsAreLeftToRequiredValidatorsAndEqualPasswordsMatch(){var validator=new PasswordsMatchValidator();assertThat(validator.isValid(null,null)).isTrue();for(var form:new Form[]{new Form(null,"valid"),new Form("valid",null),new Form(" ","valid"),new Form("valid"," "),new Form("equal","equal")})assertThat(validator.isValid(form,null)).isTrue();}
}
