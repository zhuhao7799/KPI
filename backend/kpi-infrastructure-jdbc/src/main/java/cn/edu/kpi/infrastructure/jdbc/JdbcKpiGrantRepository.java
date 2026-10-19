package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.security.KpiGrantRepository;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

/** Serializes grants per RuoYi user and preserves revocation history. */
public final class JdbcKpiGrantRepository implements KpiGrantRepository {
    private final DataSource dataSource;
    public JdbcKpiGrantRepository(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public String grantRole(String userId, KpiRole role, Instant startsAt, Instant endsAt) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockActiveUser(connection, userId);
                try (var statement = connection.prepareStatement("select id from kpi_user_role_binding where user_id=? and role_code=? and starts_at<=? and (ends_at is null or ends_at>=?) limit 1")) {
                    statement.setString(1, userId); statement.setString(2, role.name()); statement.setObject(3, startsAt); statement.setObject(4, startsAt);
                    try (var rows = statement.executeQuery()) { if (rows.next()) { connection.commit(); return rows.getString(1); } }
                }
                String id = UUID.randomUUID().toString();
                try (var statement = connection.prepareStatement("insert into kpi_user_role_binding (id,user_id,role_code,starts_at,ends_at) values (?,?,?,?,?)")) {
                    statement.setString(1, id); statement.setString(2, userId); statement.setString(3, role.name());
                    statement.setObject(4, startsAt); statement.setObject(5, endsAt); statement.executeUpdate();
                }
                connection.commit(); return id;
            } catch (Exception error) { connection.rollback(); throw error; }
        } catch (SQLException error) { throw new IllegalStateException("cannot grant KPI role", error); }
    }

    @Override public String grantScope(String userId, String cycleCode, String unitId, KpiPermission permission, Instant grantedAt, Instant expiresAt) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockActiveUser(connection, userId);
                String cycleId = id(connection, "select id from assessment_cycle where cycle_code=?", cycleCode);
                String unitSnapshotId = unitId == null || unitId.isBlank() ? null :
                    id(connection, "select id from organization_unit_snapshot where assessment_cycle_id=? and external_unit_id=?", cycleId, unitId);
                try (var statement = connection.prepareStatement("select id from kpi_scope_grant where user_id=? and assessment_cycle_id=? and organization_unit_snapshot_id <=> ? and permission_code=? and (expires_at is null or expires_at>=?) limit 1")) {
                    statement.setString(1, userId); statement.setString(2, cycleId); setNullableString(statement, 3, unitSnapshotId);
                    statement.setString(4, permission.name()); statement.setObject(5, grantedAt);
                    try (var rows = statement.executeQuery()) { if (rows.next()) { connection.commit(); return rows.getString(1); } }
                }
                String grantId = UUID.randomUUID().toString();
                try (var statement = connection.prepareStatement("insert into kpi_scope_grant (id,user_id,assessment_cycle_id,organization_unit_snapshot_id,permission_code,granted_at,expires_at) values (?,?,?,?,?,?,?)")) {
                    statement.setString(1, grantId); statement.setString(2, userId); statement.setString(3, cycleId);
                    setNullableString(statement, 4, unitSnapshotId); statement.setString(5, permission.name());
                    statement.setObject(6, grantedAt); statement.setObject(7, expiresAt); statement.executeUpdate();
                }
                connection.commit(); return grantId;
            } catch (Exception error) { connection.rollback(); throw error; }
        } catch (SQLException error) { throw new IllegalStateException("cannot grant KPI scope", error); }
    }

    @Override public void revokeRole(String bindingId, Instant revokedAt) {
        updateExpiration("update kpi_user_role_binding set ends_at=? where id=? and starts_at<=? and (ends_at is null or ends_at>?)", bindingId, revokedAt);
    }

    @Override public void revokeScope(String grantId, Instant revokedAt) {
        updateExpiration("update kpi_scope_grant set expires_at=? where id=? and granted_at<=? and (expires_at is null or expires_at>?)", grantId, revokedAt);
    }

    private void updateExpiration(String sql, String id, Instant at) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, at); statement.setString(2, id); statement.setObject(3, at); statement.setObject(4, at);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("active KPI grant was not found: " + id);
        } catch (SQLException error) { throw new IllegalStateException("cannot revoke KPI grant", error); }
    }

    private void lockActiveUser(Connection connection, String userId) throws SQLException {
        try (var statement = connection.prepareStatement("select user_id from sys_user where cast(user_id as char)=? and status='0' and del_flag='0' for update")) {
            statement.setString(1, userId);
            try (var rows = statement.executeQuery()) { if (!rows.next()) throw new IllegalArgumentException("active RuoYi user was not found: " + userId); }
        }
    }

    private String id(Connection connection, String sql, String... values) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setString(index + 1, values[index]);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalArgumentException("assessment cycle or unit was not found");
                return rows.getString(1);
            }
        }
    }

    private void setNullableString(java.sql.PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) statement.setNull(index, Types.CHAR); else statement.setString(index, value);
    }
}
