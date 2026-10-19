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

    @Override public Optional<IndicatorVersion> findPublished(String versionCode) {
        var header = "select id, version_code from indicator_version where version_code = ? and status = 'PUBLISHED'";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(header)) {
            statement.setString(1, versionCode);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                var id = rows.getString("id"); var items = new ArrayList<IndicatorVersion.Indicator>();
                try (var itemStatement = connection.prepareStatement("select indicator_code, indicator_name, weight, maximum_score from indicator_item_snapshot where indicator_version_id = ? order by sort_order")) {
                    itemStatement.setString(1, id);
                    try (var itemRows = itemStatement.executeQuery()) { while (itemRows.next()) items.add(new IndicatorVersion.Indicator(itemRows.getString(1), itemRows.getString(2), itemRows.getBigDecimal(3), itemRows.getBigDecimal(4))); }
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
                try (var item = connection.prepareStatement("insert into indicator_item_snapshot (id, indicator_version_id, indicator_code, indicator_name, weight, maximum_score, sort_order) values (uuid(), ?, ?, ?, ?, ?, ?)")) {
                    int order = 1; for (var indicator : version.indicators()) { item.setString(1, id); item.setString(2, indicator.code()); item.setString(3, indicator.name()); item.setBigDecimal(4, indicator.weight()); item.setBigDecimal(5, indicator.maximumScore()); item.setInt(6, order++); item.addBatch(); } item.executeBatch();
                }
                connection.commit();
            } catch (Exception error) { connection.rollback(); throw error; }
        } catch (SQLException error) { throw new IllegalStateException("cannot publish indicator version", error); }
    }
}
