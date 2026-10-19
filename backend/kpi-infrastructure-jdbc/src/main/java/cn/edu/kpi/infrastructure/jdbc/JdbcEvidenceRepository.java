package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.evidence.EvidenceRepository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/** Persists only private-object metadata and its access trail. */
public final class JdbcEvidenceRepository implements EvidenceRepository {
    private final DataSource dataSource;
    public JdbcEvidenceRepository(DataSource dataSource) { this.dataSource = dataSource; }
    @Override public String register(EvidenceMetadata value, String actorId, Instant at) {
        var id = UUID.randomUUID().toString();
        var sql = "insert into evidence_object (id, storage_key, original_filename, media_type, byte_size, sha256, uploaded_by, uploaded_at) values (?, ?, ?, ?, ?, ?, ?, ?)";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) { s.setString(1,id);s.setString(2,value.storageKey());s.setString(3,value.filename());s.setString(4,value.mediaType());s.setLong(5,value.byteSize());s.setString(6,value.sha256());s.setString(7,actorId);s.setObject(8,at);s.executeUpdate();return id; } catch(SQLException e){throw new IllegalStateException("cannot register evidence",e);}
    }
    @Override public void attach(String submissionId,String indicatorCode,String evidenceId,String actorId,Instant at){
        var sql="insert into submission_evidence (id,assessment_submission_id,indicator_code,evidence_object_id,attached_by,attached_at) "
            + "select ?,s.id,d.indicator_code,e.id,?,? from assessment_submission s "
            + "join assessment_score_detail d on d.assessment_submission_id=s.id and d.indicator_code=? "
            + "join evidence_object e on e.id=? and e.uploaded_by=? and e.deleted_at is null "
            + "where s.id=? and s.state in ('DRAFT','RETURNED')";
        try(var c=dataSource.getConnection()){
            c.setAutoCommit(false);
            try {
                try(var s=c.prepareStatement(sql)){
                    s.setString(1,UUID.randomUUID().toString());s.setString(2,actorId);s.setObject(3,at);
                    s.setString(4,indicatorCode);s.setString(5,evidenceId);s.setString(6,actorId);s.setString(7,submissionId);
                    if(s.executeUpdate()!=1)throw new IllegalArgumentException("material, indicator or editable submission is unavailable");
                }
                try(var count=c.prepareStatement("update assessment_score_detail set evidence_count=evidence_count+1 where assessment_submission_id=? and indicator_code=?")){
                    count.setString(1,submissionId);count.setString(2,indicatorCode);
                    if(count.executeUpdate()!=1)throw new IllegalStateException("cannot update indicator evidence count");
                }
                c.commit();
            }catch(Exception error){c.rollback();throw error;}
        }catch(SQLException e){throw new IllegalStateException("cannot attach evidence",e);}
    }
    @Override public void auditAccess(String evidenceId,String actorId,AccessAction action,String requestId,Instant at){
        var sql="insert into evidence_access_audit (id,evidence_object_id,actor_id,access_action,accessed_at,request_id) values (?,?,?,?,?,?)";
        try(var c=dataSource.getConnection();var s=c.prepareStatement(sql)){s.setString(1,UUID.randomUUID().toString());s.setString(2,evidenceId);s.setString(3,actorId);s.setString(4,action.name());s.setObject(5,at);s.setString(6,requestId);s.executeUpdate();}catch(SQLException e){throw new IllegalStateException("cannot audit evidence access",e);}
    }
    public void removeUnattached(String evidenceId, String actorId) {
        var sql = "delete from evidence_object where id=? and uploaded_by=? and not exists "
            + "(select 1 from submission_evidence where evidence_object_id=?)";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, evidenceId); statement.setString(2, actorId); statement.setString(3, evidenceId);
            statement.executeUpdate();
        } catch (SQLException error) { throw new IllegalStateException("cannot remove incomplete evidence metadata", error); }
    }
}
