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

    public record Scheme(String code, String name, String category, String status, String formulaVersion) { }
}
