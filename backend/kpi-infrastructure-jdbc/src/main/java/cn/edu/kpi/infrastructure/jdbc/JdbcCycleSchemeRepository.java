package cn.edu.kpi.infrastructure.jdbc;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class JdbcCycleSchemeRepository {
    private final DataSource dataSource;
    public JdbcCycleSchemeRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public List<Binding> list(String cycleCode) {
        var sql = "select b.organization_category,s.scheme_code,s.scheme_name,v.version_code,b.status from assessment_cycle_scheme b join assessment_cycle c on c.id=b.assessment_cycle_id join performance_scheme s on s.id=b.scheme_id join indicator_version v on v.id=b.indicator_version_id where c.cycle_code=? order by b.organization_category";
        try (var c=dataSource.getConnection(); var s=c.prepareStatement(sql)) { s.setString(1,cycleCode); try(var r=s.executeQuery()){var out=new ArrayList<Binding>(); while(r.next()) out.add(new Binding(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5))); return List.copyOf(out); } }
        catch(SQLException e){throw new IllegalStateException("cannot list cycle schemes",e);}
    }

    public void bind(String cycleCode, String category, String schemeCode, String versionCode, String operator) {
        var sql="insert into assessment_cycle_scheme (id,assessment_cycle_id,organization_category,scheme_id,indicator_version_id,status,created_by) select ?,c.id,?,s.id,v.id,'DRAFT',? from assessment_cycle c join performance_scheme s on s.scheme_code=? join indicator_version v on v.version_code=? where c.cycle_code=? and c.state in ('DRAFT','CONFIGURING')";
        try(var c=dataSource.getConnection();var s=c.prepareStatement(sql)){s.setString(1, UUID.randomUUID().toString());s.setString(2,category);s.setString(3,operator);s.setString(4,schemeCode);s.setString(5,versionCode);s.setString(6,cycleCode);if(s.executeUpdate()==0) throw new IllegalStateException("cycle, scheme or version unavailable");}
        catch(SQLException e){throw new IllegalStateException("cannot bind cycle scheme",e);}
    }

    public List<String> validate(String cycleCode) {
        var errors = new ArrayList<String>();
        try (var c=dataSource.getConnection()) {
            try (var s=c.prepareStatement("select count(*) from organization_unit_snapshot u join assessment_cycle c on c.id=u.assessment_cycle_id where c.cycle_code=?")) { s.setString(1,cycleCode); try(var r=s.executeQuery()){r.next();if(r.getInt(1)==0) errors.add("未配置组织单位");} }
            try (var s=c.prepareStatement("select organization_category from assessment_cycle_scheme b join assessment_cycle c on c.id=b.assessment_cycle_id where c.cycle_code=? and b.status='DRAFT'")) { s.setString(1,cycleCode); try(var r=s.executeQuery()){var categories=new java.util.HashSet<String>();while(r.next())categories.add(r.getString(1));if(!categories.contains("二级学院"))errors.add("未绑定二级学院方案");if(!categories.contains("职能部门"))errors.add("未绑定职能部门方案");} }
            try (var s=c.prepareStatement("select count(*) from assessment_cycle_scheme b join assessment_cycle c on c.id=b.assessment_cycle_id join indicator_version v on v.id=b.indicator_version_id where c.cycle_code=? and v.status='PUBLISHED'")) { s.setString(1,cycleCode); try(var r=s.executeQuery()){r.next();if(r.getInt(1)==0)errors.add("未绑定已发布指标版本");} }
            return List.copyOf(errors);
        } catch(SQLException e){throw new IllegalStateException("cannot validate cycle scheme",e);}
    }
    public record Binding(String category,String schemeCode,String schemeName,String indicatorVersion,String status){}
}
