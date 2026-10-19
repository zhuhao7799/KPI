package cn.edu.kpi.ruoyi;

import cn.edu.kpi.application.security.KpiIdentityResolver;
import cn.edu.kpi.application.security.KpiGrantRepository;
import cn.edu.kpi.application.security.KpiGrantAdministrationService;
import cn.edu.kpi.application.assessment.AssessmentSnapshotReader;
import cn.edu.kpi.application.assessment.AssessmentSubmissionService;
import cn.edu.kpi.application.assessment.SubmissionContextReader;
import cn.edu.kpi.application.assessment.SubmissionRepository;
import cn.edu.kpi.application.cycle.AssessmentCycleRepository;
import cn.edu.kpi.application.cycle.CycleAdministrationService;
import cn.edu.kpi.application.cycle.OrganizationUnitSnapshotRepository;
import cn.edu.kpi.application.indicator.IndicatorVersionPublicationService;
import cn.edu.kpi.application.indicator.IndicatorVersionRepository;
import cn.edu.kpi.application.result.ResultQueryRepository;
import cn.edu.kpi.application.result.ResultQueryService;
import cn.edu.kpi.application.evidence.EvidenceAccessService;
import cn.edu.kpi.application.evidence.EvidenceRepository;
import cn.edu.kpi.application.workflow.ApprovalTaskRepository;
import cn.edu.kpi.application.workflow.ApprovalTaskService;
import cn.edu.kpi.application.security.KpiScopeAuthorizationService;
import cn.edu.kpi.application.security.KpiScopeGrantRepository;
import cn.edu.kpi.infrastructure.jdbc.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.time.Clock;
import java.nio.file.Path;

/** Wires KPI identity and scope ports to the RuoYi application datasource. */
@AutoConfiguration
@Import({KpiAssessmentController.class, KpiExceptionHandler.class})
public class KpiRuoYiConfiguration {
    @Bean
    public Clock kpiClock() { return Clock.systemUTC(); }

    @Bean
    public KpiIdentityResolver kpiIdentityResolver(DataSource dataSource) { return new JdbcKpiIdentityResolver(dataSource); }

    @Bean
    public KpiScopeGrantRepository kpiScopeGrantRepository(DataSource dataSource) { return new JdbcKpiScopeGrantRepository(dataSource); }

    @Bean public KpiGrantRepository kpiGrantRepository(DataSource dataSource) { return new JdbcKpiGrantRepository(dataSource); }
    @Bean public KpiGrantAdministrationService kpiGrantAdministration(KpiGrantRepository grants, Clock kpiClock) { return new KpiGrantAdministrationService(grants, kpiClock); }

    @Bean
    public KpiScopeAuthorizationService kpiScopeAuthorizationService(KpiScopeGrantRepository grants, Clock kpiClock) {
        return new KpiScopeAuthorizationService(grants, kpiClock);
    }

    @Bean public KpiCurrentActor kpiCurrentActor(KpiIdentityResolver identities, Clock kpiClock) { return new KpiCurrentActor(identities, kpiClock); }
    @Bean public JdbcKpiCatalog kpiCatalog(DataSource dataSource) { return new JdbcKpiCatalog(dataSource); }
    @Bean public AssessmentCycleRepository kpiCycles(DataSource dataSource) { return new JdbcAssessmentCycleRepository(dataSource); }
    @Bean public OrganizationUnitSnapshotRepository kpiUnits(DataSource dataSource) { return new JdbcOrganizationUnitSnapshotRepository(dataSource); }
    @Bean public JdbcIndicatorVersionRepository kpiIndicators(DataSource dataSource) { return new JdbcIndicatorVersionRepository(dataSource); }
    @Bean public JdbcPerformanceSchemeRepository kpiPerformanceSchemes(DataSource dataSource) { return new JdbcPerformanceSchemeRepository(dataSource); }
    @Bean public JdbcCycleSchemeRepository kpiCycleSchemes(DataSource dataSource) { return new JdbcCycleSchemeRepository(dataSource); }
    @Bean public AssessmentSnapshotReader kpiSnapshots(DataSource dataSource) { return new JdbcAssessmentSnapshotReader(dataSource); }
    @Bean public SubmissionRepository kpiSubmissions(DataSource dataSource) { return new JdbcSubmissionRepository(dataSource); }
    @Bean public SubmissionContextReader kpiSubmissionContexts(DataSource dataSource) { return new JdbcSubmissionContextReader(dataSource); }
    @Bean public ResultQueryRepository kpiResultRows(DataSource dataSource) { return new JdbcResultQueryRepository(dataSource); }
    @Bean public ApprovalTaskRepository kpiApprovalTasks(DataSource dataSource) { return new JdbcApprovalTaskRepository(dataSource); }
    @Bean public JdbcEvidenceRepository kpiEvidenceRepository(DataSource dataSource) { return new JdbcEvidenceRepository(dataSource); }
    @Bean public JdbcEvidenceLookup kpiEvidenceLookup(DataSource dataSource) { return new JdbcEvidenceLookup(dataSource); }
    @Bean public PrivateEvidenceStorage kpiPrivateEvidenceStorage(@Value("${kpi.evidence.root:./data/kpi-evidence}") String root) { return new PrivateEvidenceStorage(Path.of(root)); }
    @Bean public CycleAdministrationService kpiCycleAdministration(AssessmentCycleRepository cycles, OrganizationUnitSnapshotRepository units, Clock kpiClock) { return new CycleAdministrationService(cycles, units, kpiClock); }
    @Bean public IndicatorVersionPublicationService kpiIndicatorPublication(IndicatorVersionRepository versions, Clock kpiClock) { return new IndicatorVersionPublicationService(versions, kpiClock); }
    @Bean public AssessmentSubmissionService kpiAssessmentSubmissions(AssessmentSnapshotReader snapshots, SubmissionRepository submissions, Clock kpiClock) { return new AssessmentSubmissionService(snapshots, submissions, kpiClock); }
    @Bean public ResultQueryService kpiResultQueries(ResultQueryRepository results) { return new ResultQueryService(results); }
    @Bean public ApprovalTaskService kpiApprovalTaskService(ApprovalTaskRepository tasks, AssessmentSubmissionService submissions, Clock kpiClock) { return new ApprovalTaskService(tasks, submissions, kpiClock); }
    @Bean public EvidenceAccessService kpiEvidenceAccess(EvidenceRepository repository, Clock kpiClock) { return new EvidenceAccessService(repository, kpiClock); }
}
