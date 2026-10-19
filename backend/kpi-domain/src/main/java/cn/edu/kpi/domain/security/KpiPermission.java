package cn.edu.kpi.domain.security;

/** Business permissions; framework-specific authorities will map to these values. */
public enum KpiPermission {
    CYCLE_MANAGE,
    AUTHORIZATION_MANAGE,
    INDICATOR_VERSION_MANAGE,
    SUBMISSION_CREATE,
    SUBMISSION_REVIEW,
    TASK_ASSIGN,
    SUBMISSION_CONFIRM,
    SUBMISSION_ARCHIVE,
    RESULT_VIEW,
    EVIDENCE_READ
}
