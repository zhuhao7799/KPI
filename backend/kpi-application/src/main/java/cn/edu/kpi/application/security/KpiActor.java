package cn.edu.kpi.application.security;

import cn.edu.kpi.domain.security.KpiRole;

import java.util.Set;

/** Authenticated RuoYi user mapped to KPI business roles. */
public record KpiActor(String userId, Set<KpiRole> roles) {
    public KpiActor { roles = Set.copyOf(roles); }
}
