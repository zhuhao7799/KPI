package cn.edu.kpi.domain.security;

/** Roles are scoped separately from the organization's reporting hierarchy. */
public enum KpiRole {
    SCHOOL_ADMIN,
    ASSESSMENT_OFFICE,
    UNIT_EDITOR,
    REVIEWER,
    EVALUATOR,
    RESULT_VIEWER
}
