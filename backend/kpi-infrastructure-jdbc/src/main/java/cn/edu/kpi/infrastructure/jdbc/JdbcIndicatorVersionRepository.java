package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.indicator.IndicatorVersionRepository;
import cn.edu.kpi.domain.model.IndicatorVersion;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

/** Stores a published indicator version and its items atomically. */
public final class JdbcIndicatorVersionRepository implements IndicatorVersionRepository {
    private final DataSource dataSource;
    public JdbcIndicatorVersionRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public java.util.List<VersionSummary> listVersions() {
        var sql = "select version_code, version_name, status, formula_version, score_scale, published_at from indicator_version order by published_at desc, version_code desc";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql); var rows = statement.executeQuery()) {
            var result = new java.util.ArrayList<VersionSummary>();
            while (rows.next()) result.add(new VersionSummary(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4), rows.getInt(5), rows.getTimestamp(6)));
            return java.util.List.copyOf(result);
        } catch (SQLException error) { throw new IllegalStateException("cannot list indicator versions", error); }
    }

    public record VersionSummary(String version, String name, String status, String formulaVersion, int scoreScale, java.sql.Timestamp publishedAt) { }
    public record Reference(String type, String code, String status) { }
    public record Change(String type, String code, String fromName, String toName) { }

    public java.util.List<IndicatorVersion.Indicator> items(String versionCode) {
        var sql = "select i.indicator_code,i.indicator_name,i.weight,i.maximum_score,i.indicator_category,i.indicator_type,i.measurement_unit,i.scoring_method,i.evidence_required,i.description from indicator_item_snapshot i join indicator_version v on v.id=i.indicator_version_id where v.version_code=? order by i.sort_order";
        try (var c=dataSource.getConnection(); var s=c.prepareStatement(sql)) { s.setString(1, versionCode); try (var r=s.executeQuery()) { var out=new java.util.ArrayList<IndicatorVersion.Indicator>(); while(r.next()) out.add(new IndicatorVersion.Indicator(r.getString(1),r.getString(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8),r.getBoolean(9),r.getString(10))); return java.util.List.copyOf(out); } }
        catch (SQLException e) { throw new IllegalStateException("cannot read indicator version", e); }
    }

    public void saveDraft(IndicatorVersion version, String formulaVersion, int scoreScale) {
        try (var c=dataSource.getConnection()) {
            c.setAutoCommit(false);
            String id;
            try (var q=c.prepareStatement("select id,status from indicator_version where version_code=? for update")) { q.setString(1,version.version()); try(var r=q.executeQuery()) { if(r.next()) { if("PUBLISHED".equals(r.getString(2))) throw new IllegalStateException("published indicator version is immutable"); id=r.getString(1); assertNotLocked(c,id); try(var u=c.prepareStatement("update indicator_version set version_name=?,formula_version=?,score_scale=? where id=?")){u.setString(1,version.version());u.setString(2,formulaVersion);u.setInt(3,scoreScale);u.setString(4,id);u.executeUpdate();} try(var d=c.prepareStatement("delete from indicator_item_snapshot where indicator_version_id=?")){d.setString(1,id);d.executeUpdate();} } else { id=java.util.UUID.randomUUID().toString(); try(var h=c.prepareStatement("insert into indicator_version(id,version_code,version_name,status,formula_version,score_scale) values(?,?,?,'DRAFT',?,?)")){h.setString(1,id);h.setString(2,version.version());h.setString(3,version.version());h.setString(4,formulaVersion);h.setInt(5,scoreScale);h.executeUpdate();} } } }
            try (var i=c.prepareStatement("insert into indicator_item_snapshot(id,indicator_version_id,indicator_code,indicator_name,weight,maximum_score,sort_order,indicator_category,indicator_type,measurement_unit,scoring_method,evidence_required,description) values(uuid(),?,?,?,?,?,?,?,?,?,?,?,?)")) { int n=1; for(var item:version.indicators()){i.setString(1,id);i.setString(2,item.code());i.setString(3,item.name());i.setBigDecimal(4,item.weight());i.setBigDecimal(5,item.maximumScore());i.setInt(6,n++);i.setString(7,item.category());i.setString(8,item.indicatorType());i.setString(9,item.measurementUnit());i.setString(10,item.scoringMethod());i.setBoolean(11,item.evidenceRequired());i.setString(12,item.description());i.addBatch();} i.executeBatch(); }
            c.commit();
        } catch (SQLException e) { throw new IllegalStateException("cannot save indicator draft",e); }
    }

    public void deleteDraft(String versionCode) { try(var c=dataSource.getConnection()){ c.setAutoCommit(false); String id=null; try(var q=c.prepareStatement("select id,status from indicator_version where version_code=? for update")){q.setString(1,versionCode);try(var r=q.executeQuery()){if(!r.next())throw new IllegalArgumentException("indicator version not found");if("PUBLISHED".equals(r.getString(2)))throw new IllegalStateException("published indicator version is immutable");id=r.getString(1);}} try(var q=c.prepareStatement("select count(*) from performance_scheme where indicator_version_id=?")){q.setString(1,id);try(var r=q.executeQuery()){r.next();if(r.getInt(1)>0)throw new IllegalStateException("indicator version is referenced by a performance scheme");}} assertNotLocked(c,id); try(var d=c.prepareStatement("delete from indicator_item_snapshot where indicator_version_id=?")){d.setString(1,id);d.executeUpdate();} try(var d=c.prepareStatement("delete from indicator_version where id=?")){d.setString(1,id);d.executeUpdate();} c.commit(); } catch(SQLException e){throw new IllegalStateException("cannot delete indicator draft",e);} }

    private static void assertNotLocked(java.sql.Connection c, String versionId) throws SQLException { try(var q=c.prepareStatement("select count(*) from assessment_cycle_scheme b join assessment_cycle c on c.id=b.assessment_cycle_id where b.indicator_version_id=? and c.state='LOCKED'")){q.setString(1,versionId);try(var r=q.executeQuery()){r.next();if(r.getInt(1)>0)throw new IllegalStateException("indicator version is referenced by a locked assessment cycle");}} }

    public void clonePublished(String sourceVersion, String targetVersion) {
        try (var c=dataSource.getConnection()) {
            c.setAutoCommit(false); String sourceId, formula; int scale;
            try (var q=c.prepareStatement("select id,formula_version,score_scale,status from indicator_version where version_code=?")) { q.setString(1,sourceVersion); try(var r=q.executeQuery()){ if(!r.next()) throw new IllegalArgumentException("source indicator version not found"); if(!"PUBLISHED".equals(r.getString(4))) throw new IllegalStateException("only published versions can be cloned"); sourceId=r.getString(1); formula=r.getString(2); scale=r.getInt(3); } }
            String targetId=java.util.UUID.randomUUID().toString();
            try(var h=c.prepareStatement("insert into indicator_version(id,version_code,version_name,status,formula_version,score_scale) values(?,?,?,'DRAFT',?,?)")){h.setString(1,targetId);h.setString(2,targetVersion);h.setString(3,targetVersion);h.setString(4,formula);h.setInt(5,scale);h.executeUpdate();}
            try(var i=c.prepareStatement("insert into indicator_item_snapshot(id,indicator_version_id,indicator_code,indicator_name,weight,maximum_score,sort_order,indicator_category,indicator_type,measurement_unit,scoring_method,evidence_required,description) select uuid(),?,indicator_code,indicator_name,weight,maximum_score,sort_order,indicator_category,indicator_type,measurement_unit,scoring_method,evidence_required,description from indicator_item_snapshot where indicator_version_id=?")){i.setString(1,targetId);i.setString(2,sourceId);i.executeUpdate();}
            c.commit();
        } catch(SQLException e) { throw new IllegalStateException("cannot clone indicator version",e); }
    }

    public java.util.List<Reference> references(String versionCode) {
        var sql="select 'PERFORMANCE_SCHEME',s.scheme_code,s.status from performance_scheme s join indicator_version v on v.id=s.indicator_version_id where v.version_code=? union all select 'ASSESSMENT_CYCLE',c.cycle_code,b.status from assessment_cycle_scheme b join assessment_cycle c on c.id=b.assessment_cycle_id join indicator_version v on v.id=b.indicator_version_id where v.version_code=?";
        try(var c=dataSource.getConnection();var s=c.prepareStatement(sql)){s.setString(1,versionCode);s.setString(2,versionCode);try(var r=s.executeQuery()){var out=new java.util.ArrayList<Reference>();while(r.next())out.add(new Reference(r.getString(1),r.getString(2),r.getString(3)));return java.util.List.copyOf(out);}}
        catch(SQLException e){throw new IllegalStateException("cannot list indicator references",e);}
    }

    public java.util.List<Change> compare(String fromVersion, String toVersion) {
        var left = new java.util.HashMap<String, IndicatorVersion.Indicator>(); for (var item : items(fromVersion)) left.put(item.code(), item);
        var right = new java.util.HashMap<String, IndicatorVersion.Indicator>(); for (var item : items(toVersion)) right.put(item.code(), item);
        var codes = new java.util.TreeSet<String>(); codes.addAll(left.keySet()); codes.addAll(right.keySet());
        var result = new java.util.ArrayList<Change>();
        for (var code : codes) { var a=left.get(code); var b=right.get(code); if(a==null) result.add(new Change("新增",code,null,b.name())); else if(b==null) result.add(new Change("删除",code,a.name(),null)); else if(!a.equals(b)) result.add(new Change("修改",code,a.name(),b.name())); }
        return java.util.List.copyOf(result);
    }

    public void archive(String versionCode) {
        try (var c=dataSource.getConnection(); var s=c.prepareStatement("update indicator_version set status='ARCHIVED' where version_code=? and status='PUBLISHED'")) { s.setString(1,versionCode); if(s.executeUpdate()==0) throw new IllegalStateException("only published indicator versions can be archived"); }
        catch(SQLException e){throw new IllegalStateException("cannot archive indicator version",e);}
    }

    public void audit(String eventType, String aggregateId, String actorId, String payload) {
        try (var c=dataSource.getConnection(); var s=c.prepareStatement("insert into assessment_audit_event(id,aggregate_type,aggregate_id,event_type,actor_id,occurred_at,payload_json) values(uuid(),'INDICATOR_VERSION',?,?,?,current_timestamp(6),?)")) { s.setString(1,aggregateId); s.setString(2,eventType); s.setString(3,actorId); s.setString(4,payload == null ? "{}" : payload); s.executeUpdate(); }
        catch(SQLException e){throw new IllegalStateException("cannot write indicator audit",e);}
    }

    public void publishDraft(String versionCode, java.time.Instant publishedAt) { try(var c=dataSource.getConnection()){ c.setAutoCommit(false); try(var q=c.prepareStatement("select id,status from indicator_version where version_code=? for update")){q.setString(1,versionCode);try(var r=q.executeQuery()){if(!r.next())throw new IllegalArgumentException("indicator version not found");if(!"DRAFT".equals(r.getString(2)))throw new IllegalStateException("only draft indicator versions can be published");String id=r.getString(1);try(var w=c.prepareStatement("select coalesce(sum(weight),0),count(*) from indicator_item_snapshot where indicator_version_id=?")){w.setString(1,id);try(var x=w.executeQuery()){x.next();if(x.getBigDecimal(1).compareTo(new BigDecimal("100"))!=0||x.getInt(2)==0)throw new IllegalStateException("indicator weights must total 100");}}try(var u=c.prepareStatement("update indicator_version set status='PUBLISHED',published_at=? where id=?")){u.setObject(1,publishedAt);u.setString(2,id);u.executeUpdate();}}} c.commit(); }catch(SQLException e){throw new IllegalStateException("cannot publish indicator draft",e);} }

    @Override public Optional<IndicatorVersion> findPublished(String versionCode) {
        var header = "select id, version_code from indicator_version where version_code = ? and status = 'PUBLISHED'";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(header)) {
            statement.setString(1, versionCode);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                var id = rows.getString("id"); var items = new ArrayList<IndicatorVersion.Indicator>();
                try (var itemStatement = connection.prepareStatement("select indicator_code, indicator_name, weight, maximum_score,indicator_category,indicator_type,measurement_unit,scoring_method,evidence_required,description from indicator_item_snapshot where indicator_version_id = ? order by sort_order")) {
                    itemStatement.setString(1, id);
                    try (var itemRows = itemStatement.executeQuery()) { while (itemRows.next()) items.add(new IndicatorVersion.Indicator(itemRows.getString(1), itemRows.getString(2), itemRows.getBigDecimal(3), itemRows.getBigDecimal(4),itemRows.getString(5),itemRows.getString(6),itemRows.getString(7),itemRows.getString(8),itemRows.getBoolean(9),itemRows.getString(10))); }
                }
                return Optional.of(new IndicatorVersion(rows.getString("version_code"), items));
            }
        } catch (SQLException error) { throw new IllegalStateException("cannot read indicator version", error); }
    }

    @Override public void publish(IndicatorVersion version, String formulaVersion, int scoreScale, Instant publishedAt) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String id;
                try (var header = connection.prepareStatement("insert into indicator_version (id, version_code, version_name, status, formula_version, score_scale, published_at) values (uuid(), ?, ?, 'PUBLISHED', ?, ?, ?)")) {
                    header.setString(1, version.version()); header.setString(2, version.version()); header.setString(3, formulaVersion); header.setInt(4, scoreScale); header.setObject(5, publishedAt); header.executeUpdate();
                }
                try (var find = connection.prepareStatement("select id from indicator_version where version_code = ?")) { find.setString(1, version.version()); try (var rows = find.executeQuery()) { rows.next(); id = rows.getString(1); } }
                try (var item = connection.prepareStatement("insert into indicator_item_snapshot (id, indicator_version_id, indicator_code, indicator_name, weight, maximum_score, sort_order,indicator_category,indicator_type,measurement_unit,scoring_method,evidence_required,description) values (uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    int order = 1; for (var indicator : version.indicators()) { item.setString(1, id); item.setString(2, indicator.code()); item.setString(3, indicator.name()); item.setBigDecimal(4, indicator.weight()); item.setBigDecimal(5, indicator.maximumScore()); item.setInt(6, order++); item.setString(7,indicator.category()); item.setString(8,indicator.indicatorType()); item.setString(9,indicator.measurementUnit()); item.setString(10,indicator.scoringMethod()); item.setBoolean(11,indicator.evidenceRequired()); item.setString(12,indicator.description()); item.addBatch(); } item.executeBatch();
                }
                connection.commit();
            } catch (Exception error) { connection.rollback(); throw error; }
        } catch (SQLException error) { throw new IllegalStateException("cannot publish indicator version", error); }
    }
}
