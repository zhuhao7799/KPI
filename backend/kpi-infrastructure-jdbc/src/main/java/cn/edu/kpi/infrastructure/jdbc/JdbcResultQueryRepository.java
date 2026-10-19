package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.result.ResultQueryRepository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcResultQueryRepository implements ResultQueryRepository {
    private final DataSource dataSource;
    public JdbcResultQueryRepository(DataSource dataSource) { this.dataSource = dataSource; }
    @Override public List<ResultRow> listFinalized(String cycleCode) {
        var sql = "select s.id,u.external_unit_id,u.unit_name,u.unit_category,s.total_score from assessment_submission s join assessment_cycle c on c.id=s.assessment_cycle_id join organization_unit_snapshot u on u.id=s.organization_unit_snapshot_id where c.cycle_code=? and s.state in ('CONFIRMED','ARCHIVED')";
        try (var c=dataSource.getConnection();var s=c.prepareStatement(sql)) { s.setString(1,cycleCode);try(var rows=s.executeQuery()){var result=new ArrayList<ResultRow>();while(rows.next())result.add(new ResultRow(rows.getString(1),rows.getString(2),rows.getString(3),rows.getString(4),rows.getBigDecimal(5)));return List.copyOf(result);}}catch(SQLException e){throw new IllegalStateException("cannot query assessment results",e);}
    }
}
