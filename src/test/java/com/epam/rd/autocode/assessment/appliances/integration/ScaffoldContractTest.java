package com.epam.rd.autocode.assessment.appliances.integration;

import com.epam.rd.autocode.assessment.appliances.aspect.LoggingServices;
import com.epam.rd.autocode.assessment.appliances.controller.ApplianceController;
import com.epam.rd.autocode.assessment.appliances.controller.ClientController;
import com.epam.rd.autocode.assessment.appliances.controller.EmployeeController;
import com.epam.rd.autocode.assessment.appliances.controller.IndexController;
import com.epam.rd.autocode.assessment.appliances.controller.ManufacturerController;
import com.epam.rd.autocode.assessment.appliances.controller.OrdersController;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScaffoldContractTest {

    @Test
    void unfinishedScaffoldClassesDoNotRegisterAsSpringComponents() {

        var placeholders = List.of(
                new ApplianceController(), new ClientController(), new EmployeeController(),
                new IndexController(), new ManufacturerController(), new OrdersController(),
                new LoggingServices()
        );

        assertThat(placeholders).allSatisfy(placeholder -> {
            assertThat(AnnotatedElementUtils.hasAnnotation(placeholder.getClass(), Component.class))
                    .as("%s is still a scaffold placeholder", placeholder.getClass().getSimpleName())
                    .isFalse();
        });
    }
}
