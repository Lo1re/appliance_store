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
        return configuration -> {
            var placeholders = new HashMap<>(configuration.getPlaceholders());
            placeholders.putAll(Credentials.HASHES);
            configuration.placeholders(placeholders);
        };
    }

    /** Each demo password is salted once per JVM, shared by isolated test contexts. */
    private static final class Credentials {
        private static final java.util.Map<String, String> HASHES = generate();

        private static java.util.Map<String, String> generate() {
            var encoder = new BCryptPasswordEncoder();
            var hashes = new HashMap<String, String>();
            for (String name : List.of("Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn",
                    "Uranus", "Neptune", "Phobos", "Moon", "Deimos", "Europa")) {
                hashes.put("demoPassword" + name, encoder.encode(name + "Local2026!"));
            }
            return java.util.Map.copyOf(hashes);
        }
    }
}
