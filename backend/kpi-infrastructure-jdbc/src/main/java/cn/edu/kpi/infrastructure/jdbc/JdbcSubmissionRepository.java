package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.assessment.SubmissionRepository;
import cn.edu.kpi.domain.score.ScoreCalculator;
import cn.edu.kpi.domain.submission.SubmissionState;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/** Transactional snapshot writer for draft scores and their immutable details. */
public final class JdbcSubmissionRepository implements SubmissionRepository {
    private final DataSource dataSource;
    public JdbcSubmissionRepository(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public String createDraft(ScoreCalculator.ScoreResult score, Instant occurredAt) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                var cycleId = id(connection, "select id from assessment_cycle where cycle_code = ?", score.cycleCode());
                var unitId = id(connection, "select id from organization_unit_snapshot where assessment_cycle_id = ? and external_unit_id = ?", cycleId, score.unitId());
                var versionId = id(connection, "select id from indicator_version where version_code = ? and status = 'PUBLISHED'", score.indicatorVersion());
                var submissionId = UUID.randomUUID().toString();
                var header = "insert into assessment_submission (id, assessment_cycle_id, organization_unit_snapshot_id, indicator_version_id, state, formula_version, score_scale, total_score, submitted_by, submitted_at) values (?, ?, ?, ?, 'DRAFT', ?, 4, ?, ?, ?)";
                try (var statement = connection.prepareStatement(header)) { statement.setString(1,submissionId);statement.setString(2,cycleId);statement.setString(3,unitId);statement.setString(4,versionId);statement.setString(5,"weighted-score-v1");statement.setBigDecimal(6,score.totalScore());statement.setString(7,score.calculatedBy());statement.setObject(8,occurredAt);statement.executeUpdate(); }
                var detail = "insert into assessment_score_detail (id, assessment_submission_id, indicator_code, indicator_name, raw_score, maximum_score, weight, weighted_score, evidence_count) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (var statement = connection.prepareStatement(detail)) { for (var row : score.details()) { statement.setString(1,UUID.randomUUID().toString());statement.setString(2,submissionId);statement.setString(3,row.indicatorCode());statement.setString(4,row.indicatorName());statement.setBigDecimal(5,row.rawScore());statement.setBigDecimal(6,row.maximumScore());statement.setBigDecimal(7,row.weight());statement.setBigDecimal(8,row.weightedScore());statement.setInt(9,row.evidence().size());statement.addBatch(); } statement.executeBatch(); }
                audit(connection, submissionId, "DRAFT_CREATED", score.calculatedBy(), occurredAt);
                connection.commit(); return submissionId;
            } catch (Exception error) { connection.rollback(); throw error; }
        } catch (SQLException error) { throw new IllegalStateException("cannot create assessment draft", error); }
    }
    @Override public SubmissionState stateOf(String submissionId) { try (var c=dataSource.getConnection();var s=c.prepareStatement("select state from assessment_submission where id=?")){s.setString(1,submissionId);try(var r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("unknown submission: "+submissionId);return SubmissionState.valueOf(r.getString(1));}}catch(SQLException e){throw new IllegalStateException("cannot read submission",e);} }
    @Override public void changeState(String submissionId,SubmissionState expected,SubmissionState target,String actorId,Instant at){try(var c=dataSource.getConnection()){c.setAutoCommit(false);try(var s=c.prepareStatement("update assessment_submission set state=?, submitted_by=case when ?='SUBMITTED' then ? else submitted_by end, submitted_at=case when ?='SUBMITTED' then ? else submitted_at end where id=? and state=?")){s.setString(1,target.name());s.setString(2,target.name());s.setString(3,actorId);s.setString(4,target.name());s.setObject(5,at);s.setString(6,submissionId);s.setString(7,expected.name());if(s.executeUpdate()!=1)throw new IllegalStateException("submission state changed; reload before retrying");audit(c,submissionId,"STATE_"+target.name(),actorId,at);c.commit();}catch(Exception e){c.rollback();throw e;}}catch(SQLException e){throw new IllegalStateException("cannot change submission state",e);}}
    private void audit(Connection c,String submissionId,String event,String actor,Instant at)throws SQLException{try(var s=c.prepareStatement("insert into assessment_audit_event (id,aggregate_type,aggregate_id,event_type,actor_id,occurred_at,payload_json) values (?,?,?,?,?,?,json_object())")){s.setString(1,UUID.randomUUID().toString());s.setString(2,"ASSESSMENT_SUBMISSION");s.setString(3,submissionId);s.setString(4,event);s.setString(5,actor);s.setObject(6,at);s.executeUpdate();}}
    private String id(Connection c,String sql,String... values)throws SQLException{try(var s=c.prepareStatement(sql)){for(int i=0;i<values.length;i++)s.setString(i+1,values[i]);try(var r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("required snapshot is missing");return r.getString(1);}}}
}
