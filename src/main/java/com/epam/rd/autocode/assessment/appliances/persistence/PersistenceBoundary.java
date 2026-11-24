package com.epam.rd.autocode.assessment.appliances.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Marks the outer boundary at which a completed transaction may be translated.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PersistenceBoundary {
    PersistenceOperation value();
}
