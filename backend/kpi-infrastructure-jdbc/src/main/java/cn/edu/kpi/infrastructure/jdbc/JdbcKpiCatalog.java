package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.domain.model.AssessmentCycle;
import cn.edu.kpi.domain.model.OrganizationUnit;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only lists for the KPI management screens. */
public final class JdbcKpiCatalog {
    private final DataSource dataSource;

    public JdbcKpiCatalog(DataSource dataSource) { this.dataSource = dataSource; }

    public List<AssessmentCycle> cycles() {
        var sql = "select cycle_code,cycle_name,starts_on,ends_on,state from assessment_cycle order by starts_on desc";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql); var rows = statement.executeQuery()) {
            var result = new ArrayList<AssessmentCycle>();
            while (rows.next()) result.add(new AssessmentCycle(rows.getString(1), rows.getString(2), rows.getObject(3, java.time.LocalDate.class), rows.getObject(4, java.time.LocalDate.class), AssessmentCycle.Status.valueOf(rows.getString(5))));
            return List.copyOf(result);
        } catch (SQLException error) { throw new IllegalStateException("cannot list assessment cycles", error); }
    }

    public List<OrganizationUnit> units(String cycleCode) {
        var sql = "select u.external_unit_id,u.unit_name,u.unit_category,u.parent_external_unit_id "
            + "from organization_unit_snapshot u join assessment_cycle c on c.id=u.assessment_cycle_id "
            + "where c.cycle_code=? order by u.unit_category,u.unit_name";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, cycleCode);
            try (var rows = statement.executeQuery()) {
                var result = new ArrayList<OrganizationUnit>();
                while (rows.next()) result.add(new OrganizationUnit(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4)));
                return List.copyOf(result);
            }
        } catch (SQLException error) { throw new IllegalStateException("cannot list assessment units", error); }
    }
}
