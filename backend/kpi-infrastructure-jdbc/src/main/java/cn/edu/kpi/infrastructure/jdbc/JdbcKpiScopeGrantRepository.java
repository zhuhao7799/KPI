package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.security.KpiScopeGrantRepository;
import cn.edu.kpi.domain.security.KpiPermission;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;

/** Matches a user grant to one cycle and either the whole cycle or a unit snapshot. */
public final class JdbcKpiScopeGrantRepository implements KpiScopeGrantRepository {
    private final DataSource dataSource;

    public JdbcKpiScopeGrantRepository(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public boolean isGranted(String userId, String cycleCode, String unitId, KpiPermission permission, Instant at) {
        var sql = "select 1 from kpi_scope_grant g join assessment_cycle c on c.id=g.assessment_cycle_id "
            + "left join organization_unit_snapshot u on u.id=g.organization_unit_snapshot_id "
            + "where g.user_id=? and c.cycle_code=? and g.permission_code=? and g.granted_at<=? "
            + "and (g.expires_at is null or g.expires_at>=?) and (g.organization_unit_snapshot_id is null or u.external_unit_id=?) limit 1";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId); statement.setString(2, cycleCode); statement.setString(3, permission.name());
            statement.setObject(4, at); statement.setObject(5, at); statement.setString(6, unitId);
            try (var rows = statement.executeQuery()) { return rows.next(); }
        } catch (SQLException error) { throw new IllegalStateException("cannot resolve KPI data scope", error); }
    }
}
