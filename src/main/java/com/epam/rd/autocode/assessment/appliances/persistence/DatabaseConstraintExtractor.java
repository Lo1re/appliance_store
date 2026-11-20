package com.epam.rd.autocode.assessment.appliances.persistence;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConstraintExtractor {
    private static final Pattern H2_CONSTRAINT = Pattern.compile(
            "^(?:Unique index or primary key violation|Referential integrity constraint violation): \"(?:[A-Za-z0-9_]+\\.)?([A-Za-z0-9_]+)(?=[: ]|\")");

    public List<ConstraintFailure> extract(RuntimeException failure) {
        var failures = new ArrayList<ConstraintFailure>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var pending = new ArrayDeque<Throwable>();
        pending.add(failure);
        while (!pending.isEmpty() && seen.size() < 32) {
            Throwable current = pending.removeFirst();
            if (!seen.add(current)) {
                continue;
            }

            if (current instanceof ConstraintViolationException constraint) {
                failures.add(new ConstraintFailure(constraint.getSQLState(), constraint.getConstraintName()));
            } else if (current instanceof SQLException sql) {
                // PostgreSQL names arrive through Hibernate's structured constraint metadata.
                // H2 reports index names only inside its stable exception header.
                if (sql.getClass().getName().startsWith("org.h2.jdbc.") && sql.getMessage() != null) {
                    var matcher = H2_CONSTRAINT.matcher(sql.getMessage());
                    if (matcher.find()) {
                        failures.add(new ConstraintFailure(sql.getSQLState(), matcher.group(1)));
                    }
                }
                if (sql.getNextException() != null) {
                    pending.addLast(sql.getNextException());
                }
            }
            if (current.getCause() != null) {
                pending.addLast(current.getCause());
            }
        }
        return List.copyOf(failures);
    }

}
