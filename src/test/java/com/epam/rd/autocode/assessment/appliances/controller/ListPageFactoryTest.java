package com.epam.rd.autocode.assessment.appliances.controller;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import static org.assertj.core.api.Assertions.*;

class ListPageFactoryTest {
    @Test
    void clampsNegativePagesAndUsesTheResourceDefaultSort() {
        var query = new ListQuery(); query.setPage(-1); query.setSort("password");
        var page = new ListPageFactory(15).create(query, Set.of("name", "id"));
        assertThat(page.pageable().getPageNumber()).isZero();
        assertThat(page.pageable().getPageSize()).isEqualTo(15);
        assertThat(page.pageable().getSort()).isEqualTo(Sort.by("name", "id"));
        query.setSort(null);
        assertThat(new ListPageFactory(10).create(query, Set.of("id"), "id").sort()).isEqualTo("id");
    }

    @Test
    void addsIdentityOrderingOnlyWhenNeededAndRejectsInvalidConfiguration() {
        var query = new ListQuery(); query.setSort("id"); query.setPage(3);
        assertThat(new ListPageFactory(10).create(query, Set.of("id"), "id").pageable().getSort())
                .isEqualTo(Sort.by("id"));
        assertThatThrownBy(() -> new ListPageFactory(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ListPageFactory(101)).isInstanceOf(IllegalArgumentException.class);
    }
}
