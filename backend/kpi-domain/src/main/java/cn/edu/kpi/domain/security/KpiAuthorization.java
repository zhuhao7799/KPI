package cn.edu.kpi.domain.security;

import java.util.Map;
import java.util.Set;

/** Central policy used by APIs before invoking domain state transitions. */
public final class KpiAuthorization {
    private static final Map<KpiRole, Set<KpiPermission>> GRANTS = Map.of(
        KpiRole.SCHOOL_ADMIN, Set.of(KpiPermission.values()),
        KpiRole.ASSESSMENT_OFFICE, Set.of(KpiPermission.CYCLE_MANAGE, KpiPermission.INDICATOR_VERSION_MANAGE, KpiPermission.SUBMISSION_REVIEW, KpiPermission.TASK_ASSIGN, KpiPermission.SUBMISSION_CONFIRM, KpiPermission.SUBMISSION_ARCHIVE, KpiPermission.RESULT_VIEW, KpiPermission.EVIDENCE_READ),
        KpiRole.UNIT_EDITOR, Set.of(KpiPermission.SUBMISSION_CREATE, KpiPermission.RESULT_VIEW, KpiPermission.EVIDENCE_READ),
        KpiRole.REVIEWER, Set.of(KpiPermission.SUBMISSION_REVIEW, KpiPermission.SUBMISSION_CONFIRM, KpiPermission.RESULT_VIEW, KpiPermission.EVIDENCE_READ),
        KpiRole.EVALUATOR, Set.of(KpiPermission.SUBMISSION_REVIEW, KpiPermission.SUBMISSION_CONFIRM, KpiPermission.RESULT_VIEW, KpiPermission.EVIDENCE_READ),
        KpiRole.RESULT_VIEWER, Set.of(KpiPermission.RESULT_VIEW));

    private KpiAuthorization() { }

    public static boolean isAllowed(Set<KpiRole> roles, KpiPermission permission) {
        if (roles == null || permission == null) return false;
        return roles.stream().anyMatch(role -> GRANTS.getOrDefault(role, Set.of()).contains(permission));
    }

    public static void require(Set<KpiRole> roles, KpiPermission permission) {
        if (!isAllowed(roles, permission)) throw new SecurityException("missing permission: " + permission);
    }
}
