package com.epam.rd.autocode.assessment.appliances.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.epam.rd.autocode.assessment.appliances.exception.OrderException;

import org.junit.jupiter.api.Test;

import java.util.List;

class OrdersLifecycleTest {

    @Test
    void pendingOrderCanReplaceRowsWithoutReplacingItsCollection() {
        var order = new Orders();
        var rows = order.getOrderRowSet();
        var previous = new OrderRow();
        var replacement = new OrderRow();
        rows.add(previous);

        order.replaceRows(List.of(replacement));

        assertThat(order.getOrderRowSet()).isSameAs(rows).containsExactly(replacement);
        assertThat(order.getApproved()).isFalse();
        assertThat(order.getEmployee()).isNull();
        assertThatCode(order::assertEditable).doesNotThrowAnyException();
    }

    @Test
    void replacingRowsWithTheSameCollectionPreservesItsContents() {
        var order = new Orders();
        var row = new OrderRow();
        order.getOrderRowSet().add(row);

        order.replaceRows(order.getOrderRowSet());

        assertThat(order.getOrderRowSet()).containsExactly(row);
    }

    @Test
    void approvalStoresEmployeeAndPreventsFurtherChanges() {
        var order = new Orders();
        var employee = new Employee();
        var row = new OrderRow();
        order.replaceRows(List.of(row));

        order.approve(employee);

        assertThat(order.getApproved()).isTrue();
        assertThat(order.getEmployee()).isSameAs(employee);
        assertThatThrownBy(order::assertEditable)
                .isInstanceOf(OrderException.class)
                .hasMessage("order.not-editable");
        assertThatThrownBy(() -> order.replaceRows(List.of(new OrderRow())))
                .isInstanceOf(OrderException.class)
                .hasMessage("order.not-editable");
        assertThat(order.getOrderRowSet()).containsExactly(row);
    }

    @Test
    void repeatedApprovalPreservesOriginalEmployee() {
        var order = new Orders();
        var employee = new Employee();
        order.approve(employee);

        assertThatThrownBy(() -> order.approve(new Employee()))
                .isInstanceOf(OrderException.class)
                .hasMessage("order.already-approved");
        assertThat(order.getEmployee()).isSameAs(employee);
        assertThat(order.getApproved()).isTrue();
    }

    @Test
    void missingEmployeeDoesNotPartiallyApproveOrder() {
        var order = new Orders();

        assertThatThrownBy(() -> order.approve(null)).isInstanceOf(NullPointerException.class);
        assertThat(order.getApproved()).isFalse();
        assertThat(order.getEmployee()).isNull();
    }
}
