package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.cycle.AssessmentCycleRepository;
import cn.edu.kpi.domain.model.AssessmentCycle;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** JDBC adapter; RuoYi's datasource supplies the DataSource at runtime. */
public final class JdbcAssessmentCycleRepository implements AssessmentCycleRepository {
    private final DataSource dataSource;

    public JdbcAssessmentCycleRepository(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public Optional<AssessmentCycle> findByCode(String cycleCode) {
        var sql = "select cycle_code, cycle_name, starts_on, ends_on, state from assessment_cycle where cycle_code = ?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, cycleCode);
            try (var rows = statement.executeQuery()) { return rows.next() ? Optional.of(map(rows)) : Optional.empty(); }
        } catch (SQLException error) { throw new IllegalStateException("cannot read assessment cycle", error); }
    }

    @Override public void insert(AssessmentCycle cycle) {
        var sql = "insert into assessment_cycle (id, cycle_code, cycle_name, starts_on, ends_on, state) values (uuid(), ?, ?, ?, ?, ?)";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, cycle.code()); statement.setString(2, cycle.name());
            statement.setObject(3, cycle.startsOn()); statement.setObject(4, cycle.endsOn()); statement.setString(5, cycle.status().name());
            if (statement.executeUpdate() != 1) throw new IllegalStateException("cycle insert did not affect one row");
        } catch (SQLException error) { throw new IllegalStateException("cannot create assessment cycle", error); }
    }

    @Override public void updateStatus(String cycleCode, AssessmentCycle.Status target) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("update assessment_cycle set state = ? where cycle_code = ?")) {
            statement.setString(1, target.name()); statement.setString(2, cycleCode);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("unknown assessment cycle: " + cycleCode);
        } catch (SQLException error) { throw new IllegalStateException("cannot update assessment cycle", error); }
    }

    private AssessmentCycle map(ResultSet rows) throws SQLException {
        return new AssessmentCycle(rows.getString("cycle_code"), rows.getString("cycle_name"), rows.getObject("starts_on", java.time.LocalDate.class), rows.getObject("ends_on", java.time.LocalDate.class), AssessmentCycle.Status.valueOf(rows.getString("state")));
    }
}
