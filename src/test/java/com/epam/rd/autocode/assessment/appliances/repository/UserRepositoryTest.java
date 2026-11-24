package com.epam.rd.autocode.assessment.appliances.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@org.springframework.context.annotation.Import(
        com.epam.rd.autocode.assessment.appliances.config.demo.DemoMigrationConfiguration.class)
@DataJpaTest
class UserRepositoryTest {
    @Autowired UserRepository users;

    @Test
    void joinedUsersResolvePolymorphicallyAndEmailCheckCanExcludeOwner() {
        var mercury = users.findByEmailIgnoreCase("MERCURY@GMAIL.COM").orElseThrow();
        var phobos = users.findByEmailIgnoreCase("PHOBOS@GMAIL.COM").orElseThrow();

        assertThat(mercury).isInstanceOf(Client.class);
        assertThat(phobos).isInstanceOf(Employee.class);
        assertThat(users.existsByEmailIgnoreCase("MERCURY@GMAIL.COM")).isTrue();
        assertThat(users.existsByEmailIgnoreCaseAndIdNot("MERCURY@GMAIL.COM", mercury.getId()))
                .isFalse();
        assertThat(users.existsByEmailIgnoreCaseAndIdNot("MERCURY@GMAIL.COM", phobos.getId()))
                .isTrue();
    }
}
