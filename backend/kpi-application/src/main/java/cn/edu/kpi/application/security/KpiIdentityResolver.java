package cn.edu.kpi.application.security;

import java.time.Instant;

/** Bridge from framework login identity to time-bounded KPI roles. */
public interface KpiIdentityResolver {
    KpiActor resolve(String frameworkUserId, Instant at);
}
