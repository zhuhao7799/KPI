package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.security.KpiActor;
import cn.edu.kpi.application.security.KpiIdentityResolver;
import cn.edu.kpi.domain.security.KpiRole;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;

/** Resolves RuoYi roles first, with time-bounded KPI bindings as an optional supplement. */
public final class JdbcKpiIdentityResolver implements KpiIdentityResolver {
    private final DataSource dataSource;

    public JdbcKpiIdentityResolver(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public KpiActor resolve(String frameworkUserId, Instant at) {
        var roles = EnumSet.noneOf(KpiRole.class);
        var sql = "select role_code from (" +
            "select role_code from kpi_user_role_binding where user_id = ? and starts_at <= ? and (ends_at is null or ends_at >= ?) " +
            "union all " +
            "select upper(r.role_key) from sys_user_role ur join sys_role r on r.role_id = ur.role_id " +
            "where cast(ur.user_id as char)=? and r.status='0' and r.del_flag='0'" +
            ") effective_roles";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, frameworkUserId); statement.setObject(2, at); statement.setObject(3, at); statement.setString(4, frameworkUserId);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    String role = rows.getString(1);
                    if ("ADMIN".equals(role) || "SYS_ADMIN".equals(role)) role = KpiRole.SCHOOL_ADMIN.name();
                    try { roles.add(KpiRole.valueOf(role)); } catch (IllegalArgumentException ignored) { /* generic RuoYi role */ }
                }
            }
            return new KpiActor(frameworkUserId, roles);
        } catch (SQLException error) { throw new IllegalStateException("cannot resolve KPI identity", error); }
    }
}
