package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.repository.EmployeeRepository;
import com.epam.rd.autocode.assessment.appliances.service.EmployeeService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:employee-deletion-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@WithUserDetails("phobos@gmail.com")
@Transactional
class EmployeeDeletionIntegrationTest {
    @Autowired EmployeeService service;
    @Autowired EmployeeRepository employees;
    @Autowired MockMvc mvc;

    @Test
    void selfDeletionIs409AndDoesNotRemoveTheAccount() throws Exception {
        Employee actor = employee("phobos@gmail.com");
        long before = employees.count();
        mvc.perform(post("/employees/{id}/delete", actor.getId()).with(csrf())
                        .cookie(new Cookie("store-lang", "uk")))
                .andExpect(status().isConflict())
                .andExpect(model().attribute("errorMessageKey", "employee.self-delete"))
                .andExpect(content().string(containsString("HTTP 409")))
                .andExpect(content().string(containsString("Ви не можете видалити власний")));
        assertThat(employees.count()).isEqualTo(before);
        assertThat(employees.existsById(actor.getId())).isTrue();
    }

    @Test
    void lastEmployeeCannotBeDeletedEvenByTheirOwnAccount() throws Exception {
        Employee actor = employee("phobos@gmail.com");
        employees.findAll().stream().filter(employee -> !employee.getId().equals(actor.getId()))
                .forEach(employees::delete);
        employees.flush();
        mvc.perform(post("/employees/{id}/delete", actor.getId()).with(csrf())
                        .cookie(new Cookie("store-lang", "uk")))
                .andExpect(status().isConflict())
                .andExpect(model().attribute("errorMessageKey", "employee.last-employee"))
                .andExpect(content().string(containsString("Не можна видалити останнього працівника.")));
        assertThat(employees.count()).isEqualTo(1);
        assertThat(employees.existsById(actor.getId())).isTrue();
    }

    @Test
    void anotherUnreferencedEmployeeCanStillBeDeleted() {
        Long target = employee("moon@gmail.com").getId();
        service.delete(target);
        assertThat(employees.existsById(target)).isFalse();
        assertThat(employees.existsById(employee("phobos@gmail.com").getId())).isTrue();
    }

    @Test
    void nonexistentEmployeeKeepsTheNotFoundContract() {
        assertThatThrownBy(() -> service.delete(Long.MAX_VALUE))
                .isInstanceOfSatisfying(AccountException.class,
                        error -> assertThat(error.error()).isEqualTo(AccountError.EMPLOYEE_NOT_FOUND));
    }

    private Employee employee(String email) {
        return employees.findAll().stream().filter(employee -> employee.getEmail().equals(email))
                .findFirst().orElseThrow();
    }
}
