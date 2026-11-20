package com.epam.rd.autocode.assessment.appliances.persistence;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import com.epam.rd.autocode.assessment.appliances.exception.BusinessException;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogError;
import com.epam.rd.autocode.assessment.appliances.exception.CatalogException;
import com.epam.rd.autocode.assessment.appliances.exception.OrderError;
import com.epam.rd.autocode.assessment.appliances.exception.OrderException;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Translates only named, expected database constraints for the current operation. */
@Component
public class PersistenceConflictTranslator {
    private final DatabaseConstraintExtractor extractor;

    public PersistenceConflictTranslator(DatabaseConstraintExtractor extractor) {
        this.extractor = extractor;
    }

    public RuntimeException translate(PersistenceOperation operation, RuntimeException failure) {
        for (ConstraintFailure constraint : extractor.extract(failure)) {
            BusinessException translated = classify(operation, constraint.state(), constraint.name());
            if (translated != null) {
                translated.initCause(failure);
                return translated;
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
