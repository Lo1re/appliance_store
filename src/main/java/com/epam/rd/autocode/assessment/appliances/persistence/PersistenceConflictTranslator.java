package com.epam.rd.autocode.assessment.appliances.persistence;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

/** Translates only named, expected database constraints for the current operation. */
@Component
public class PersistenceConflictTranslator {
    private static final Pattern H2_CONSTRAINT = Pattern.compile(
            "^(?:Unique index or primary key violation|Referential integrity constraint violation): \"(?:[A-Za-z0-9_]+\\.)?([A-Za-z0-9_]+)(?=[: ]|\")");

    public RuntimeException translate(PersistenceOperation operation, RuntimeException failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var pending = new ArrayDeque<Throwable>();
        pending.add(failure);
        while (!pending.isEmpty() && seen.size() < 32) {
            Throwable current = pending.removeFirst();
            if (!seen.add(current)) {
                continue;
            }
            BusinessException translated = null;
            if (current instanceof ConstraintViolationException constraint) {
                translated = classify(operation, constraint.getSQLState(), constraint.getConstraintName());
            } else if (current instanceof SQLException sql) {
                // PostgreSQL names arrive through Hibernate's structured constraint metadata.
                // H2 reports index names only inside its stable exception header.
                if (sql.getClass().getName().startsWith("org.h2.jdbc.") && sql.getMessage() != null) {
                    var matcher = H2_CONSTRAINT.matcher(sql.getMessage());
                    if (matcher.find()) {
                        translated = classify(operation, sql.getSQLState(), matcher.group(1));
                    }
                }
                if (sql.getNextException() != null) {
                    pending.addLast(sql.getNextException());
                }
            }
            if (translated != null) {
                translated.initCause(failure);
                return translated;
            }
            if (current.getCause() != null) {
                pending.addLast(current.getCause());
            }
        }
        return failure;
    }

    private BusinessException classify(PersistenceOperation operation, String state, String constraint) {
        if (constraint == null) {
            return null;
        }
        String name = constraint.toLowerCase(Locale.ROOT);
        if ("23505".equals(state)) {
            if (operation == PersistenceOperation.ACCOUNT_WRITE && unique(name, "uq_users_email")) {
                return new AccountException(AccountError.EMAIL_DUPLICATE);
            }
            if (operation == PersistenceOperation.MANUFACTURER_WRITE && unique(name, "uq_manufacturer_name")) {
                return new CatalogException(CatalogError.MANUFACTURER_DUPLICATE_NAME);
            }
        }
        if (!"23503".equals(state) && !"23506".equals(state)) {
            return null;
        }
        return switch (operation) {
            case CLIENT_DELETE -> name.equals("fk_orders_client")
                    ? new AccountException(AccountError.CLIENT_IN_USE) : null;
            case EMPLOYEE_DELETE -> name.equals("fk_orders_employee")
                    ? new AccountException(AccountError.EMPLOYEE_IN_USE) : null;
            case MANUFACTURER_DELETE -> name.equals("fk_appliance_manufacturer")
                    ? new CatalogException(CatalogError.MANUFACTURER_IN_USE) : null;
            case APPLIANCE_DELETE -> name.equals("fk_order_row_appliance")
                    ? new CatalogException(CatalogError.APPLIANCE_IN_USE) : null;
            case APPLIANCE_WRITE -> name.equals("fk_appliance_manufacturer")
                    ? new CatalogException(CatalogError.APPLIANCE_MANUFACTURER_NOT_FOUND) : null;
            case ORDER_WRITE -> switch (name) {
                case "fk_orders_client" -> new OrderException(OrderError.CLIENT_NOT_FOUND);
                case "fk_order_row_appliance" -> new OrderException(OrderError.APPLIANCE_NOT_FOUND);
                case "fk_orders_employee" -> new OrderException(OrderError.EMPLOYEE_UNAVAILABLE);
                default -> null;
            };
            default -> null;
        };
    }

    private boolean unique(String name, String expected) {
        return name.equals(expected) || name.matches(expected + "_index_[0-9a-f]+");
    }
}
