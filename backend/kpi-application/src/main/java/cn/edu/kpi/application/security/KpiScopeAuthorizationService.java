package cn.edu.kpi.application.security;

import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.time.Clock;
import java.util.Objects;

/** Combines a KPI business role with the applicable cycle and unit data scope. */
public final class KpiScopeAuthorizationService {
    private final KpiScopeGrantRepository grants;
    private final Clock clock;

    public KpiScopeAuthorizationService(KpiScopeGrantRepository grants, Clock clock) {
        this.grants = Objects.requireNonNull(grants);
        this.clock = Objects.requireNonNull(clock);
    }

    public void require(KpiActor actor, String cycleCode, String unitId, KpiPermission permission) {
        Objects.requireNonNull(actor); Objects.requireNonNull(cycleCode); Objects.requireNonNull(unitId); Objects.requireNonNull(permission);
        KpiAuthorization.require(actor.roles(), permission);
        if (!canAccess(actor, cycleCode, unitId, permission)) {
            throw new SecurityException("missing KPI data scope: " + permission + " for " + cycleCode + "/" + unitId);
        }
    }

    public boolean canAccess(KpiActor actor, String cycleCode, String unitId, KpiPermission permission) {
        Objects.requireNonNull(actor); Objects.requireNonNull(cycleCode); Objects.requireNonNull(unitId); Objects.requireNonNull(permission);
        if (!KpiAuthorization.isAllowed(actor.roles(), permission)) return false;
        if (actor.roles().contains(KpiRole.SCHOOL_ADMIN) || actor.roles().contains(KpiRole.ASSESSMENT_OFFICE)) return true;
        return grants.isGranted(actor.userId(), cycleCode, unitId, permission, clock.instant());
    }
}
