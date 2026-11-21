package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientRequest;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeRequest;
import com.epam.rd.autocode.assessment.appliances.dto.user.PasswordRequest;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.ClientService;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:catalog-accounts-it",
})
@WithUserDetails("phobos@gmail.com")
class AccountLifecycleIntegrationTest {
    private static final PageRequest PAGE = PageRequest.of(0, 30, Sort.by("name"));
    @Autowired ClientService clients;
    @Autowired EmployeeService employees;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test
    void accountCrudNormalizesEmailAndPreservesPasswordOnEdit() {
        var client = clients.create(client("  SHOPPER@EXAMPLE.COM ", " Buyer "), password());
        var employee = employees.create(employee(" WORKER@EXAMPLE.COM ", " Stock "), password());
        Long clientId = client.getId();
        Long employeeId = employee.getId();
        assertThat(client.getEmail()).isEqualTo("shopper@example.com");
        assertThat(employee.getDepartment()).isEqualTo("Stock");
        String hash = users.findById(clientId).orElseThrow().getPassword();
        assertThat(hash).isNotEqualTo(password().getPassword());
        assertThat(encoder.matches(password().getPassword(), hash)).isTrue();
        assertThat(encoder.matches(password().getPassword(),
                users.findById(employeeId).orElseThrow().getPassword())).isTrue();

        assertThat(clients.findAll(null, PAGE)).extracting(c -> c.getId()).contains(clientId);
        assertThat(clients.findAll(" ", PAGE)).extracting(c -> c.getId()).contains(clientId);
        assertThat(clients.findAll(" SHOPPER ", PAGE)).extracting(c -> c.getId()).contains(clientId);
        assertThat(employees.findAll(null, PAGE)).extracting(e -> e.getId()).contains(employeeId);
        assertThat(employees.findAll(" ", PAGE)).extracting(e -> e.getId()).contains(employeeId);
        assertThat(employees.findAll(" WORKER ", PAGE)).extracting(e -> e.getId()).contains(employeeId);

        assertThatThrownBy(() -> employees.create(employee("SHOPPER@example.com", "Stock"), password()))
                .isInstanceOf(AccountException.class).hasMessage("user.email.duplicate");
        assertThatThrownBy(() -> clients.update(clientId, client("worker@example.com", "Buyer")))
                .isInstanceOf(AccountException.class).hasMessage("user.email.duplicate");
        clients.update(clientId, client("SHOPPER@example.com", "Renamed Buyer"));
        employees.update(employeeId, employee("WORKER@example.com", "Dispatch"));
        assertThat(clients.findById(clientId).getName()).isEqualTo("Renamed Buyer");
        assertThat(employees.findById(employeeId).getDepartment()).isEqualTo("Dispatch");
        assertThat(users.findById(clientId).orElseThrow().getPassword()).isEqualTo(hash);

        clients.delete(clientId);
        employees.delete(employeeId);
        assertThatThrownBy(() -> clients.findById(clientId))
                .isInstanceOf(AccountException.class).hasMessage("client.not-found");
        assertThatThrownBy(() -> employees.findById(employeeId))
                .isInstanceOf(AccountException.class).hasMessage("employee.not-found");
    }

    @Test
    void optionalCardsAreUniqueWhenPresentAndCanBeRetainedOrClearedOnEdit() {
        var a = client("card-first@example.test", "First"); a.setCard("  UNIQUE-LOYALTY  ");
        var first = clients.create(a, password());
        var b = client("card-second@example.test", "Second"); b.setCard(null);
        var second = clients.create(b, password());
        var c = client("card-third@example.test", "Third"); c.setCard("   ");
        var third = clients.create(c, password());
        assertThat(second.getCard()).isNull(); assertThat(third.getCard()).isNull();
        clients.update(first.getId(), a);
        b.setCard(" UNIQUE-LOYALTY ");
        assertThatThrownBy(() -> clients.update(second.getId(), b)).isInstanceOf(AccountException.class).hasMessage("client.card.duplicate");
        var duplicate = client("card-duplicate@example.test", "Duplicate"); duplicate.setCard("UNIQUE-LOYALTY");
        assertThatThrownBy(() -> clients.create(duplicate, password())).isInstanceOf(AccountException.class).hasMessage("client.card.duplicate");
        a.setCard(null); clients.update(first.getId(), a);
        assertThat(clients.findById(first.getId()).getCard()).isNull();
        clients.update(second.getId(), b);
        clients.delete(first.getId()); clients.delete(second.getId()); clients.delete(third.getId());
    }

    private ClientRequest client(String email, String name) {
        var request = new ClientRequest();
        request.setName(name);
        request.setEmail(email);
        request.setCard(" TEST-CARD ");
        return request;
    }

    private EmployeeRequest employee(String email, String department) {
        var request = new EmployeeRequest();
        request.setName("Test Employee");
        request.setEmail(email);
        request.setDepartment(department);
        return request;
    }

    private PasswordRequest password() {
        var request = new PasswordRequest();
        request.setPassword("Testing2026!Valid");
        return request;
    }
}
