package cn.edu.kpi.application.security;

import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.time.Instant;

/** Writes time-bounded KPI role and assessment scope bindings. */
public interface KpiGrantRepository {
    String grantRole(String userId, KpiRole role, Instant startsAt, Instant endsAt);
    String grantScope(String userId, String cycleCode, String unitId, KpiPermission permission, Instant grantedAt, Instant expiresAt);
    void revokeRole(String bindingId, Instant revokedAt);
    void revokeScope(String grantId, Instant revokedAt);
}
