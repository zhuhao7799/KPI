package cn.edu.kpi.application.security;

import cn.edu.kpi.domain.security.KpiPermission;

import java.time.Instant;

/** Reads time-bounded KPI data-scope grants using stable external unit identifiers. */
public interface KpiScopeGrantRepository {
    boolean isGranted(String userId, String cycleCode, String unitId, KpiPermission permission, Instant at);
}
