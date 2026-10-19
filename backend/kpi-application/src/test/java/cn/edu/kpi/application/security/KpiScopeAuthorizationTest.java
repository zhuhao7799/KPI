package cn.edu.kpi.application.security;

import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

/** Executable check for role and data-scope enforcement without a framework runtime. */
public final class KpiScopeAuthorizationTest {
    public static void main(String[] args) {
        var clock = Clock.fixed(Instant.parse("2026-10-18T00:00:00Z"), ZoneOffset.UTC);
        var service = new KpiScopeAuthorizationService((user, cycle, unit, permission, at) -> user.equals("editor") && cycle.equals("2026") && unit.equals("college-a") && permission == KpiPermission.SUBMISSION_CREATE, clock);
        service.require(new KpiActor("editor", Set.of(KpiRole.UNIT_EDITOR)), "2026", "college-a", KpiPermission.SUBMISSION_CREATE);
        service.require(new KpiActor("admin", Set.of(KpiRole.SCHOOL_ADMIN)), "2026", "college-b", KpiPermission.RESULT_VIEW);
        try {
            service.require(new KpiActor("editor", Set.of(KpiRole.UNIT_EDITOR)), "2026", "college-b", KpiPermission.SUBMISSION_CREATE);
            throw new AssertionError("out-of-scope edit was allowed");
        } catch (SecurityException expected) { System.out.println("scope-authorization=passed"); }
    }
}
