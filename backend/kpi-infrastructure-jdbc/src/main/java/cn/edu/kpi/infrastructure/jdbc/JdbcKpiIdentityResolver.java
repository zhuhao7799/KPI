package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.security.KpiActor;
import cn.edu.kpi.application.security.KpiIdentityResolver;
import cn.edu.kpi.domain.security.KpiRole;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;

/** Reads active KPI role bindings that are separate from RuoYi's generic roles. */
public final class JdbcKpiIdentityResolver implements KpiIdentityResolver {
    private final DataSource dataSource;

    public JdbcKpiIdentityResolver(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public KpiActor resolve(String frameworkUserId, Instant at) {
        var roles = EnumSet.noneOf(KpiRole.class);
        var sql = "select role_code from kpi_user_role_binding where user_id = ? and starts_at <= ? and (ends_at is null or ends_at >= ?)";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, frameworkUserId); statement.setObject(2, at); statement.setObject(3, at);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) roles.add(KpiRole.valueOf(rows.getString(1)));
            }
            return new KpiActor(frameworkUserId, roles);
        } catch (SQLException error) { throw new IllegalStateException("cannot resolve KPI identity", error); }
    }
}
