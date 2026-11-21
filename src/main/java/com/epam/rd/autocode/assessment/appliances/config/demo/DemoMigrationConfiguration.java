package com.epam.rd.autocode.assessment.appliances.config.demo;

import java.util.HashMap;
import java.util.List;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration(proxyBeanMethods = false)
@Profile("(local | test) & !prod")
public class DemoMigrationConfiguration {
    @Bean
    FlywayConfigurationCustomizer demoPasswordPlaceholders() {
        var encoder = new BCryptPasswordEncoder();
        var credentials = new HashMap<String, String>();
        for (String name : List.of("Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn",
                "Uranus", "Neptune", "Phobos", "Moon", "Deimos", "Europa")) {
            credentials.put("demoPassword" + name, encoder.encode(name + "Local2026!"));
        }
        return configuration -> {
            var placeholders = new HashMap<>(configuration.getPlaceholders());
            placeholders.putAll(credentials);
            configuration.placeholders(placeholders);
        };
    }
}
