package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.assessment.AssessmentSnapshotReader;
import cn.edu.kpi.domain.model.AssessmentCycle;
import cn.edu.kpi.domain.model.IndicatorVersion;
import cn.edu.kpi.domain.model.OrganizationUnit;

import javax.sql.DataSource;
import java.sql.SQLException;

/** Reads the exact cycle, unit and published indicator snapshots used for scoring. */
public final class JdbcAssessmentSnapshotReader implements AssessmentSnapshotReader {
    private final DataSource dataSource;
    private final JdbcAssessmentCycleRepository cycles;
    private final JdbcIndicatorVersionRepository indicators;

    public JdbcAssessmentSnapshotReader(DataSource dataSource) {
        this.dataSource = dataSource;
        cycles = new JdbcAssessmentCycleRepository(dataSource);
        indicators = new JdbcIndicatorVersionRepository(dataSource);
    }

    @Override public AssessmentCycle cycle(String cycleCode) {
        return cycles.findByCode(cycleCode).orElseThrow(() -> new IllegalArgumentException("unknown assessment cycle: " + cycleCode));
    }

    @Override public OrganizationUnit unit(String cycleCode, String unitId) {
        var sql = "select u.external_unit_id,u.unit_name,u.unit_category,u.parent_external_unit_id "
            + "from organization_unit_snapshot u join assessment_cycle c on c.id=u.assessment_cycle_id "
            + "where c.cycle_code=? and u.external_unit_id=?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, cycleCode); statement.setString(2, unitId);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalArgumentException("unknown assessment unit: " + cycleCode + "/" + unitId);
                return new OrganizationUnit(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4));
            }
        } catch (SQLException error) { throw new IllegalStateException("cannot read assessment unit", error); }
    }

    @Override public IndicatorVersion indicatorVersion(String versionCode) {
        return indicators.findPublished(versionCode).orElseThrow(() -> new IllegalArgumentException("unknown published indicator version: " + versionCode));
    }
}
