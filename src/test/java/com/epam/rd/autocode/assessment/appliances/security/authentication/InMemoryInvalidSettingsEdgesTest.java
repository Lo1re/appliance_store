package com.epam.rd.autocode.assessment.appliances.security.authentication;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class InMemoryInvalidSettingsEdgesTest {
 @Test void absentAndPartialAccountEntriesAreRejected(){var env=new MockEnvironment();env.setActiveProfiles("test");var validator=new InMemoryAuthenticationValidator(mock(UserRepository.class));var properties=new InMemoryAuthenticationProperties();properties.setAccounts(null);assertThatThrownBy(()->validator.validate(properties,env)).isInstanceOf(IllegalArgumentException.class);properties.setAccounts(Arrays.asList((InMemoryAuthenticationProperties.Account)null));assertThatThrownBy(()->validator.validate(properties,env)).isInstanceOf(IllegalArgumentException.class);for(String email:new String[]{null," ","user@example.test"}){var account=new InMemoryAuthenticationProperties.Account();account.setEmail(email);properties.setAccounts(List.of(account));assertThatThrownBy(()->validator.validate(properties,env)).isInstanceOf(IllegalArgumentException.class);}}
}
