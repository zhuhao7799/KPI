package cn.edu.kpi.infrastructure.jdbc;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** JDBC persistence for performance scheme drafts. */
public final class JdbcPerformanceSchemeRepository {
    private final DataSource dataSource;
    public JdbcPerformanceSchemeRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public List<Scheme> list() {
        var sql = "select s.scheme_code,s.scheme_name,s.organization_category,s.status,s.formula_version,coalesce(v.version_code,'') from performance_scheme s left join indicator_version v on v.id=s.indicator_version_id order by s.created_at desc";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql); var r = s.executeQuery()) {
            var result = new ArrayList<Scheme>();
            while (r.next()) result.add(new Scheme(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6)));
            return List.copyOf(result);
        } catch (SQLException e) { throw new IllegalStateException("cannot list performance schemes", e); }
    }

    public void create(String code, String name, String category, String indicatorVersion, String operator) {
        var sql = "insert into performance_scheme (id,scheme_code,scheme_name,organization_category,indicator_version_id,status,formula_version,created_by) select uuid(),?,?,?,v.id,'DRAFT','weighted-score-v1',? from indicator_version v where v.version_code=? and v.status='PUBLISHED'";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) {
            s.setString(1, code); s.setString(2, name); s.setString(3, category); s.setString(4, operator); s.setString(5, indicatorVersion);
            if (s.executeUpdate() == 0) throw new IllegalArgumentException("published indicator version not found: " + indicatorVersion);
        } catch (SQLException e) { throw new IllegalStateException("cannot create performance scheme", e); }
    }

    public void deleteDraft(String code) {
        try (var c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (var check = c.prepareStatement("select id,status from performance_scheme where scheme_code=? for update")) {
                check.setString(1, code);
                try (var r = check.executeQuery()) {
                    if (!r.next()) throw new IllegalArgumentException("performance scheme not found: " + code);
                    if (!"DRAFT".equals(r.getString(2))) throw new IllegalStateException("only draft schemes can be deleted");
                    try (var locked = c.prepareStatement("select count(*) from assessment_cycle_scheme b join assessment_cycle c on c.id=b.assessment_cycle_id where b.scheme_id=? and c.state='LOCKED'")) { locked.setString(1, r.getString(1)); try (var lr=locked.executeQuery()) { lr.next(); if (lr.getInt(1)>0) throw new IllegalStateException("scheme is referenced by a locked assessment cycle"); } }
                    try (var items = c.prepareStatement("delete from performance_scheme_indicator where scheme_id=?")) { items.setString(1, r.getString(1)); items.executeUpdate(); }
                    try (var scheme = c.prepareStatement("delete from performance_scheme where id=?")) { scheme.setString(1, r.getString(1)); scheme.executeUpdate(); }
                }
            }
            c.commit();
        } catch (SQLException e) { throw new IllegalStateException("cannot delete performance scheme", e); }
    }

    public void updateDraft(String code, String name, String category, String indicatorVersion) {
        var sql = "update performance_scheme s join indicator_version v on v.version_code=? and v.status='PUBLISHED' set s.scheme_name=?, s.organization_category=?, s.indicator_version_id=v.id, s.updated_at=current_timestamp(6) where s.scheme_code=? and s.status='DRAFT'";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) {
            s.setString(1, indicatorVersion); s.setString(2, name); s.setString(3, category); s.setString(4, code);
            if (s.executeUpdate() == 0) throw new IllegalStateException("draft scheme or published indicator version not found");
        } catch (SQLException e) { throw new IllegalStateException("cannot update performance scheme", e); }
    }

    public void publishDraft(String code) {
        try (var c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (var check = c.prepareStatement("select s.id,s.status from performance_scheme s join indicator_version v on v.id=s.indicator_version_id and v.status='PUBLISHED' where s.scheme_code=? for update")) {
                check.setString(1, code);
                try (var r = check.executeQuery()) {
                    if (!r.next()) throw new IllegalArgumentException("performance scheme not found: " + code);
                    if (!"DRAFT".equals(r.getString(2))) throw new IllegalStateException("only draft schemes can be published");
                    try (var count = c.prepareStatement("select coalesce(sum(weight),0) from performance_scheme_indicator where scheme_id=?")) {
                        count.setString(1, r.getString(1));
                        try (var weights = count.executeQuery()) { if (!weights.next() || weights.getBigDecimal(1).compareTo(new java.math.BigDecimal("100")) != 0) throw new IllegalStateException("scheme indicator weights must total 100"); }
                    }
                    try (var update = c.prepareStatement("update performance_scheme set status='PUBLISHED',updated_at=current_timestamp(6) where id=?")) { update.setString(1, r.getString(1)); update.executeUpdate(); }
                }
            }
            c.commit();
        } catch (SQLException e) { throw new IllegalStateException("cannot publish performance scheme", e); }
    }

    public List<SchemeIndicator> indicators(String code) {
        var sql = "select i.indicator_code,i.indicator_name,i.weight,i.maximum_score from performance_scheme_indicator i join performance_scheme s on s.id=i.scheme_id where s.scheme_code=? order by i.sort_order";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) {
            s.setString(1, code); try (var r = s.executeQuery()) { var out = new ArrayList<SchemeIndicator>(); while (r.next()) out.add(new SchemeIndicator(r.getString(1), r.getString(2), r.getBigDecimal(3), r.getBigDecimal(4))); return List.copyOf(out); }
        } catch (SQLException e) { throw new IllegalStateException("cannot list scheme indicators", e); }
    }

    public void addIndicator(String code, String indicatorCode, java.math.BigDecimal weight) {
        var sql = "insert into performance_scheme_indicator (id,scheme_id,indicator_code,indicator_name,weight,maximum_score,sort_order) select uuid(),s.id,i.indicator_code, i.indicator_name, ?, i.maximum_score, coalesce((select max(x.sort_order)+1 from performance_scheme_indicator x where x.scheme_id=s.id),1) from performance_scheme s join indicator_item_snapshot i on i.indicator_version_id=s.indicator_version_id and i.indicator_code=? where s.scheme_code=? and s.status='DRAFT'";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) { s.setBigDecimal(1, weight); s.setString(2, indicatorCode); s.setString(3, code); if (s.executeUpdate() == 0) throw new IllegalStateException("draft scheme or indicator not found in its published version"); }
        catch (SQLException e) { throw new IllegalStateException("cannot add scheme indicator", e); }
    }

    public void updateIndicator(String schemeCode, String indicatorCode, java.math.BigDecimal weight) {
        var sql = "update performance_scheme_indicator i join performance_scheme s on s.id=i.scheme_id set i.weight=? where s.scheme_code=? and s.status='DRAFT' and i.indicator_code=?";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) { s.setBigDecimal(1, weight); s.setString(2, schemeCode); s.setString(3, indicatorCode); if (s.executeUpdate() == 0) throw new IllegalStateException("draft indicator not found"); }
        catch (SQLException e) { throw new IllegalStateException("cannot update scheme indicator", e); }
    }

    public void deleteIndicator(String schemeCode, String indicatorCode) {
        var sql = "delete i from performance_scheme_indicator i join performance_scheme s on s.id=i.scheme_id where s.scheme_code=? and s.status='DRAFT' and i.indicator_code=?";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) { s.setString(1, schemeCode); s.setString(2, indicatorCode); if (s.executeUpdate() == 0) throw new IllegalStateException("draft indicator not found"); }
        catch (SQLException e) { throw new IllegalStateException("cannot delete scheme indicator", e); }
    }

    public record SchemeIndicator(String code, String name, java.math.BigDecimal weight, java.math.BigDecimal maximumScore) { }

    public record Scheme(String code, String name, String category, String status, String formulaVersion, String indicatorVersion) { }
}
