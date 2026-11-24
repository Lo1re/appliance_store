package com.epam.rd.autocode.assessment.appliances.web;

import static org.assertj.core.api.Assertions.*;

import jakarta.servlet.DispatcherType;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class LanguageNavigationTest {
    @Test
    void preservesOnlyListFiltersAndNeverCopiesCredentials() {
        var request = new MockHttpServletRequest("GET", "/appliances");
        request.setParameter("search", "name + %_&");
        request.setParameter("sort", "price");
        request.setParameter("page", "2");
        request.setParameter("password", "private");
        request.setParameter("token", "private");
        var links = new LanguageNavigation().links(request);
        assertThat(links.uk())
                .startsWith("/appliances?lang=uk")
                .contains("search=name+%2B+%25_%26", "sort=price", "page=2")
                .doesNotContain("password", "token", "private");
        assertThat(links.en()).contains("lang=en");
    }

    @Test
    void errorDispatchAndUnsafePathsUseHomeAndOversizedFiltersAreDropped() {
        var request = new MockHttpServletRequest("GET", "//outside.example");
        request.setParameter("search", "x".repeat(101));
        assertThat(new LanguageNavigation().links(request).uk()).isEqualTo("/?lang=uk");
        request.setRequestURI("/error");
        request.setDispatcherType(DispatcherType.ERROR);
        assertThat(new LanguageNavigation().links(request).en()).isEqualTo("/?lang=en");
    }
}
