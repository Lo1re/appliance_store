package com.epam.rd.autocode.assessment.appliances.config;

import jakarta.servlet.http.Cookie;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebConfigTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var config = new WebConfig();
        mvc = MockMvcBuilders.standaloneSetup(new LocaleProbeController())
                .setLocaleResolver(config.localeResolver())
                .addInterceptors(config.localeChangeInterceptor())
                .build();
    }

    @Test
    void defaultsToEnglishAndSwitchesBetweenSupportedLanguages() throws Exception {
        mvc.perform(get("/test-locale"))
                .andExpect(status().isOk())
                .andExpect(content().string("en"));

        mvc.perform(get("/test-locale").param("lang", "uk"))
                .andExpect(content().string("uk"))
                .andExpect(header().string("Set-Cookie", startsWith("store-lang=uk")));

        mvc.perform(get("/test-locale").cookie(new Cookie("store-lang", "uk")))
                .andExpect(content().string("uk"));

        mvc.perform(get("/test-locale")
                        .cookie(new Cookie("store-lang", "uk"))
                        .param("lang", "en"))
                .andExpect(content().string("en"))
                .andExpect(header().string("Set-Cookie", startsWith("store-lang=en")));
    }

    @Test
    void unsupportedLanguageKeepsCurrentSelection() throws Exception {
        mvc.perform(get("/test-locale")
                        .cookie(new Cookie("store-lang", "uk"))
                        .param("lang", "fr"))
                .andExpect(status().isOk())
                .andExpect(content().string("uk"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @RestController
    static class LocaleProbeController {
        @GetMapping("/test-locale")
        String locale(Locale locale) {
            return locale.getLanguage();
        }
    }
}
