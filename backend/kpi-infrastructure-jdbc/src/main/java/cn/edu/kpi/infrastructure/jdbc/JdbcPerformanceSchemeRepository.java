package cn.edu.kpi.infrastructure.jdbc;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JDBC persistence for performance scheme drafts. */
public final class JdbcPerformanceSchemeRepository {
    private final DataSource dataSource;
    public JdbcPerformanceSchemeRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public List<Scheme> list() {
        var sql = "select scheme_code,scheme_name,organization_category,status,formula_version from performance_scheme order by created_at desc";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql); var r = s.executeQuery()) {
            var result = new ArrayList<Scheme>();
            while (r.next()) result.add(new Scheme(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5)));
            return List.copyOf(result);
        } catch (SQLException e) { throw new IllegalStateException("cannot list performance schemes", e); }
    }

    public void create(String code, String name, String category, String operator) {
        var sql = "insert into performance_scheme (id,scheme_code,scheme_name,organization_category,status,formula_version,created_by) values (?,?,?,?, 'DRAFT','weighted-score-v1',?)";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) {
            s.setString(1, UUID.randomUUID().toString()); s.setString(2, code); s.setString(3, name); s.setString(4, category); s.setString(5, operator); s.executeUpdate();
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
                    try (var items = c.prepareStatement("delete from performance_scheme_indicator where scheme_id=?")) { items.setString(1, r.getString(1)); items.executeUpdate(); }
                    try (var scheme = c.prepareStatement("delete from performance_scheme where id=?")) { scheme.setString(1, r.getString(1)); scheme.executeUpdate(); }
                }
            }
            c.commit();
        } catch (SQLException e) { throw new IllegalStateException("cannot delete performance scheme", e); }
    }

    public void updateDraft(String code, String name, String category) {
        var sql = "update performance_scheme set scheme_name=?, organization_category=?, updated_at=current_timestamp(6) where scheme_code=? and status='DRAFT'";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) {
            s.setString(1, name); s.setString(2, category); s.setString(3, code);
            if (s.executeUpdate() == 0) throw new IllegalStateException("only existing draft schemes can be edited");
        } catch (SQLException e) { throw new IllegalStateException("cannot update performance scheme", e); }
    }

    public void publishDraft(String code) {
        try (var c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (var check = c.prepareStatement("select id,status from performance_scheme where scheme_code=? for update")) {
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

    public void addIndicator(String code, String indicatorCode, String name, java.math.BigDecimal weight, java.math.BigDecimal maximumScore) {
        var sql = "insert into performance_scheme_indicator (id,scheme_id,indicator_code,indicator_name,weight,maximum_score,sort_order) select uuid(),id,?,?,?,?,coalesce((select max(x.sort_order)+1 from performance_scheme_indicator x where x.scheme_id=performance_scheme.id),1) from performance_scheme where scheme_code=? and status='DRAFT'";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) { s.setString(1, indicatorCode); s.setString(2, name); s.setBigDecimal(3, weight); s.setBigDecimal(4, maximumScore); s.setString(5, code); if (s.executeUpdate() == 0) throw new IllegalStateException("draft scheme not found"); }
        catch (SQLException e) { throw new IllegalStateException("cannot add scheme indicator", e); }
    }

    public void updateIndicator(String schemeCode, String indicatorCode, String name, java.math.BigDecimal weight, java.math.BigDecimal maximumScore) {
        var sql = "update performance_scheme_indicator i join performance_scheme s on s.id=i.scheme_id set i.indicator_name=?,i.weight=?,i.maximum_score=? where s.scheme_code=? and s.status='DRAFT' and i.indicator_code=?";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) { s.setString(1, name); s.setBigDecimal(2, weight); s.setBigDecimal(3, maximumScore); s.setString(4, schemeCode); s.setString(5, indicatorCode); if (s.executeUpdate() == 0) throw new IllegalStateException("draft indicator not found"); }
        catch (SQLException e) { throw new IllegalStateException("cannot update scheme indicator", e); }
    }

    public void deleteIndicator(String schemeCode, String indicatorCode) {
        var sql = "delete i from performance_scheme_indicator i join performance_scheme s on s.id=i.scheme_id where s.scheme_code=? and s.status='DRAFT' and i.indicator_code=?";
        try (var c = dataSource.getConnection(); var s = c.prepareStatement(sql)) { s.setString(1, schemeCode); s.setString(2, indicatorCode); if (s.executeUpdate() == 0) throw new IllegalStateException("draft indicator not found"); }
        catch (SQLException e) { throw new IllegalStateException("cannot delete scheme indicator", e); }
    }

    public record SchemeIndicator(String code, String name, java.math.BigDecimal weight, java.math.BigDecimal maximumScore) { }

    public record Scheme(String code, String name, String category, String status, String formulaVersion) { }
}
