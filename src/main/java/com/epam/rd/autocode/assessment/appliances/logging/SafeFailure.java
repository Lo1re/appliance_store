package com.epam.rd.autocode.assessment.appliances.logging;

import java.util.IdentityHashMap;
import java.util.Map;

public final class SafeFailure {
    private SafeFailure() {
    }

    public static Throwable snapshot(Throwable failure) {
        return copy(failure, new IdentityHashMap<>(), 0);
    }

    private static Throwable copy(Throwable failure, Map<Throwable, Boolean> visited, int depth) {
        if (failure == null || depth >= 8 || visited.put(failure, Boolean.TRUE) != null) {
            return null;
        }
        var safe = new RuntimeException(failure.getClass().getName());
        safe.setStackTrace(failure.getStackTrace());
        Throwable cause = copy(failure.getCause(), visited, depth + 1);
        if (cause != null) {
            safe.initCause(cause);
        }
        return safe;
    }
}
