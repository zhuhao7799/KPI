package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.assessment.SubmissionContextReader;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class JdbcSubmissionContextReader implements SubmissionContextReader {
    private final DataSource dataSource;

    public JdbcSubmissionContextReader(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public Context context(String submissionId) {
        var sql = "select c.cycle_code,u.external_unit_id from assessment_submission s "
            + "join assessment_cycle c on c.id=s.assessment_cycle_id "
            + "join organization_unit_snapshot u on u.id=s.organization_unit_snapshot_id where s.id=?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, submissionId);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalArgumentException("unknown submission: " + submissionId);
                return new Context(rows.getString(1), rows.getString(2));
            }
        } catch (SQLException error) { throw new IllegalStateException("cannot read submission context", error); }
    }
}
