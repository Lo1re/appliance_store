package com.epam.rd.autocode.assessment.appliances.config;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        var resolver = new CookieLocaleResolver("store-lang");
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookiePath("/");
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    LocaleChangeInterceptor localeChangeInterceptor() {
        var interceptor = new LocaleChangeInterceptor() {
            @Override
            protected Locale parseLocaleValue(String value) {
                return switch (value) {
                    case "en" -> Locale.ENGLISH;
                    case "uk" -> Locale.forLanguageTag("uk");
                    default -> throw new IllegalArgumentException("Unsupported language");
                };
            }
        };

        interceptor.setParamName("lang");
        interceptor.setHttpMethods("GET");
        interceptor.setIgnoreInvalidLocale(true);
        return interceptor;
    }
}
