package com.epam.rd.autocode.assessment.appliances.web.pagination;

import static org.assertj.core.api.Assertions.*;

import com.epam.rd.autocode.assessment.appliances.dto.query.ListQuery;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Set;

class ListPageFactoryTest {
    @Test
    void clampsNegativePagesAndUsesTheResourceDefaultSort() {
        var query = new ListQuery();
        query.setPage(-1);
        query.setSort("password");
        var page =
                new ListPageFactory(15)
                        .create(
                                query.getPage(),
                                null,
                                query.getSort(),
                                Set.of("name", "id"),
                                "name");
        assertThat(page.pageable().getPageNumber()).isZero();
        assertThat(page.pageable().getPageSize()).isEqualTo(15);
        assertThat(page.pageable().getSort()).isEqualTo(Sort.by("name", "id"));
        query.setSort(null);
        assertThat(
                        new ListPageFactory(10)
                                .create(query.getPage(), null, query.getSort(), Set.of("id"), "id")
                                .sort())
                .isEqualTo("id");
    }

    @Test
    void addsIdentityOrderingOnlyWhenNeededAndRejectsInvalidConfiguration() {
        var query = new ListQuery();
        query.setSort("id");
        query.setPage(3);
        assertThat(
                        new ListPageFactory(10)
                                .create(query.getPage(), null, query.getSort(), Set.of("id"), "id")
                                .pageable()
                                .getSort())
                .isEqualTo(Sort.by("id"));
        assertThatThrownBy(() -> new ListPageFactory(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ListPageFactory(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void explicitApiSizeOverridesTheSharedDefaultAndInvalidSizesAreRejected() {
        var factory = new ListPageFactory(15);
        assertThat(
                        factory.create(0, 5, "price", Set.of("id", "name", "price"), "name")
                                .pageable()
                                .getPageSize())
                .isEqualTo(5);
        assertThatThrownBy(() -> factory.create(0, 101, null, Set.of("name"), "name"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
