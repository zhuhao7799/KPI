package cn.edu.kpi.infrastructure.jdbc;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Resolves private material metadata and every submission to which it is attached. */
public final class JdbcEvidenceLookup {
    private final DataSource dataSource;
    public JdbcEvidenceLookup(DataSource dataSource) { this.dataSource = dataSource; }

    public List<EvidenceAccessRow> accessRows(String evidenceId) {
        var sql = "select e.storage_key,e.original_filename,e.media_type,c.cycle_code,u.external_unit_id "
            + "from evidence_object e join submission_evidence se on se.evidence_object_id=e.id "
            + "join assessment_submission s on s.id=se.assessment_submission_id "
            + "join assessment_cycle c on c.id=s.assessment_cycle_id "
            + "join organization_unit_snapshot u on u.id=s.organization_unit_snapshot_id "
            + "where e.id=? and e.deleted_at is null";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, evidenceId);
            try (var rows = statement.executeQuery()) {
                var result = new ArrayList<EvidenceAccessRow>();
                while (rows.next()) result.add(new EvidenceAccessRow(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4), rows.getString(5)));
                return List.copyOf(result);
            }
        } catch (SQLException error) { throw new IllegalStateException("cannot resolve private evidence access", error); }
    }

    public record EvidenceAccessRow(String storageKey, String filename, String mediaType, String cycleCode, String unitId) { }
}
