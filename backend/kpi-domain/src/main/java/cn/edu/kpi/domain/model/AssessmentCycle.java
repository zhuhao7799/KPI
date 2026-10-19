package cn.edu.kpi.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/** A school-configured period for one organizational assessment. */
public record AssessmentCycle(String code, String name, LocalDate startsOn, LocalDate endsOn, Status status) {
    public AssessmentCycle {
        requireText(code, "cycle code");
        requireText(name, "cycle name");
        Objects.requireNonNull(startsOn, "startsOn");
        Objects.requireNonNull(endsOn, "endsOn");
        Objects.requireNonNull(status, "status");
        if (endsOn.isBefore(startsOn)) {
            throw new IllegalArgumentException("cycle end must not precede its start");
        }
    }

    public enum Status { DRAFT, ACTIVE, LOCKED, ARCHIVED }

    static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
