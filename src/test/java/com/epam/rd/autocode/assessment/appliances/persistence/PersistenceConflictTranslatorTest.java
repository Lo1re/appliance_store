package com.epam.rd.autocode.assessment.appliances.persistence;

import com.epam.rd.autocode.assessment.appliances.exception.AccountError;
import com.epam.rd.autocode.assessment.appliances.exception.AccountException;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PersistenceConflictTranslatorTest {
    private final PersistenceConflictTranslator translator = new PersistenceConflictTranslator(new DatabaseConstraintExtractor());

    @Test
    void translatesOnlyTheNamedConstraintInTheCorrectOperation() {
        var failure = failure("23505", "uq_users_email");
        var translated = translator.translate(PersistenceOperation.ACCOUNT_WRITE, failure);
        assertThat(translated).isInstanceOf(AccountException.class).hasCause(failure);
        assertThat(((AccountException) translated).error()).isEqualTo(AccountError.EMAIL_DUPLICATE);
        assertThat(translator.translate(PersistenceOperation.ORDER_WRITE, failure)).isSameAs(failure);
    }

    @Test
    void keepsUnknownConstraintsStatesAndMissingMetadataUnchanged() {
        for (var failure : new RuntimeException[]{failure("23505", "unknown"),
                failure("23502", "uq_users_email"), failure(null, "uq_users_email"),
                failure("23505", null), new RuntimeException(new SQLException("uq_users_email", "23505"))}) {
            assertThat(translator.translate(PersistenceOperation.ACCOUNT_WRITE, failure)).isSameAs(failure);
        }
    }

    private RuntimeException failure(String state, String name) {
        return new RuntimeException(new ConstraintViolationException(
                "private database details", new SQLException("private values", state), name));
    }
}
