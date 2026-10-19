package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.cycle.OrganizationUnitSnapshotRepository;
import cn.edu.kpi.domain.model.OrganizationUnit;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

/** Replaces the draft cycle's unit scope in one transaction. */
public final class JdbcOrganizationUnitSnapshotRepository implements OrganizationUnitSnapshotRepository {
    private final DataSource dataSource;

    public JdbcOrganizationUnitSnapshotRepository(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public void replaceForDraftCycle(String cycleCode, List<OrganizationUnit> units, Instant capturedAt) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                var cycleId = cycleId(connection, cycleCode);
                try (var delete = connection.prepareStatement("delete from organization_unit_snapshot where assessment_cycle_id = ?")) {
                    delete.setString(1, cycleId); delete.executeUpdate();
                }
                var insertSql = "insert into organization_unit_snapshot (id, assessment_cycle_id, external_unit_id, unit_name, unit_category, parent_external_unit_id, captured_at) values (uuid(), ?, ?, ?, ?, ?, ?)";
                try (var insert = connection.prepareStatement(insertSql)) {
                    for (var unit : units) {
                        insert.setString(1, cycleId); insert.setString(2, unit.id()); insert.setString(3, unit.name()); insert.setString(4, unit.category());
                        insert.setString(5, unit.parentId()); insert.setObject(6, capturedAt); insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (Exception error) { connection.rollback(); throw error; }
        } catch (SQLException error) { throw new IllegalStateException("cannot replace organization unit snapshots", error); }
    }

    private String cycleId(java.sql.Connection connection, String cycleCode) throws SQLException {
        try (var statement = connection.prepareStatement("select id from assessment_cycle where cycle_code = ? for update")) {
            statement.setString(1, cycleCode);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalArgumentException("unknown assessment cycle: " + cycleCode);
                return rows.getString(1);
            }
        }
    }
}
