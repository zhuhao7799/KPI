package cn.edu.kpi.application.security;

import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** School administrators manage business roles and unit scopes independently of RuoYi menus. */
public final class KpiGrantAdministrationService {
    private final KpiGrantRepository grants;
    private final Clock clock;

    public KpiGrantAdministrationService(KpiGrantRepository grants, Clock clock) {
        this.grants = Objects.requireNonNull(grants); this.clock = Objects.requireNonNull(clock);
    }

    public String grantRole(KpiActor administrator, String userId, KpiRole role, Instant endsAt) {
        KpiAuthorization.require(administrator.roles(), KpiPermission.AUTHORIZATION_MANAGE);
        if (userId == null || userId.isBlank() || role == null) throw new IllegalArgumentException("target user and role are required");
        Instant now = clock.instant();
        if (endsAt != null && !endsAt.isAfter(now)) throw new IllegalArgumentException("role expiration must be in the future");
        return grants.grantRole(userId, role, now, endsAt);
    }

    public String grantScope(KpiActor administrator, String userId, String cycleCode, String unitId,
                             KpiPermission permission, Instant expiresAt) {
        KpiAuthorization.require(administrator.roles(), KpiPermission.AUTHORIZATION_MANAGE);
        if (userId == null || userId.isBlank() || cycleCode == null || cycleCode.isBlank() || permission == null)
            throw new IllegalArgumentException("target user, cycle and permission are required");
        if (permission == KpiPermission.AUTHORIZATION_MANAGE || permission == KpiPermission.CYCLE_MANAGE)
            throw new IllegalArgumentException("global administration is granted by role, not unit scope");
        Instant now = clock.instant();
        if (expiresAt != null && !expiresAt.isAfter(now)) throw new IllegalArgumentException("scope expiration must be in the future");
        return grants.grantScope(userId, cycleCode, unitId, permission, now, expiresAt);
    }

    public void revokeRole(KpiActor administrator, String bindingId) {
        KpiAuthorization.require(administrator.roles(), KpiPermission.AUTHORIZATION_MANAGE);
        grants.revokeRole(bindingId, clock.instant());
    }

    public void revokeScope(KpiActor administrator, String grantId) {
        KpiAuthorization.require(administrator.roles(), KpiPermission.AUTHORIZATION_MANAGE);
        grants.revokeScope(grantId, clock.instant());
    }
}
