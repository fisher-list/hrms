package com.hrms.common.employee.service;

import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * State machine for employee status transitions.
 *
 * <p>Valid transitions:
 * PENDING_HIRE -> PROBATION -> ACTIVE -> ON_LEAVE / TERMINATED
 * ON_LEAVE -> ACTIVE / TERMINATED
 * TERMINATED is terminal (irreversible).</p>
 */
public final class EmployeeStateMachine {

    private static final Map<String, Set<String>> TRANSITIONS = new HashMap<>();

    static {
        TRANSITIONS.put("PENDING_HIRE", Set.of("PROBATION"));
        TRANSITIONS.put("PROBATION", Set.of("ACTIVE"));
        TRANSITIONS.put("ACTIVE", Set.of("ON_LEAVE", "TERMINATED"));
        TRANSITIONS.put("ON_LEAVE", Set.of("ACTIVE", "TERMINATED"));
        TRANSITIONS.put("TERMINATED", Set.of()); // irreversible
    }

    private EmployeeStateMachine() {
    }

    /**
     * Validate that the given state transition is allowed.
     *
     * @param from current status
     * @param to   target status
     * @throws BizException if the transition is invalid
     */
    public static void validate(String from, String to) {
        Set<String> allowed = TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BizException(BizCode.EMPLOYEE_INVALID_STATE_TRANSITION,
                    "Invalid state transition: " + from + " -> " + to);
        }
    }
}
