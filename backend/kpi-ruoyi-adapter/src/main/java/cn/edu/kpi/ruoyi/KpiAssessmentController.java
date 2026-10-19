package cn.edu.kpi.ruoyi;

import cn.edu.kpi.application.assessment.AssessmentSubmissionService;
import cn.edu.kpi.application.assessment.SubmissionContextReader;
import cn.edu.kpi.application.cycle.CycleAdministrationService;
import cn.edu.kpi.application.indicator.IndicatorVersionPublicationService;
import cn.edu.kpi.application.result.ResultQueryService;
import cn.edu.kpi.application.evidence.EvidenceAccessService;
import cn.edu.kpi.application.evidence.EvidenceRepository;
import cn.edu.kpi.application.workflow.ApprovalTaskRepository;
import cn.edu.kpi.application.workflow.ApprovalTaskService;
import cn.edu.kpi.application.security.KpiScopeAuthorizationService;
import cn.edu.kpi.application.security.KpiIdentityResolver;
import cn.edu.kpi.application.security.KpiGrantAdministrationService;
import cn.edu.kpi.application.security.KpiActor;
import cn.edu.kpi.domain.model.AssessmentCycle;
import cn.edu.kpi.domain.model.IndicatorVersion;
import cn.edu.kpi.domain.model.OrganizationUnit;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;
import cn.edu.kpi.domain.submission.SubmissionState;
import cn.edu.kpi.infrastructure.jdbc.JdbcKpiCatalog;
import cn.edu.kpi.infrastructure.jdbc.JdbcIndicatorVersionRepository;
import cn.edu.kpi.infrastructure.jdbc.JdbcPerformanceSchemeRepository;
import cn.edu.kpi.infrastructure.jdbc.JdbcCycleSchemeRepository;
import cn.edu.kpi.infrastructure.jdbc.JdbcEvidenceLookup;
import cn.edu.kpi.infrastructure.jdbc.JdbcEvidenceRepository;
import cn.edu.kpi.application.assessment.AssessmentSnapshotReader;
import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Authenticated KPI endpoints backed by the application services and MySQL snapshots. */
@RestController
@RequestMapping("/kpi")
public final class KpiAssessmentController {
    private final KpiCurrentActor current;
    private final KpiIdentityResolver identities;
    private final Clock clock;
    private final KpiGrantAdministrationService grants;
    private final KpiScopeAuthorizationService scope;
    private final JdbcKpiCatalog catalog;
    private final JdbcIndicatorVersionRepository indicatorRepository;
    private final JdbcPerformanceSchemeRepository schemeRepository;
    private final JdbcCycleSchemeRepository cycleSchemeRepository;
    private final CycleAdministrationService cycles;
    private final IndicatorVersionPublicationService publication;
    private final AssessmentSnapshotReader snapshots;
    private final AssessmentSubmissionService submissions;
    private final SubmissionContextReader contexts;
    private final ResultQueryService results;
    private final ApprovalTaskService tasks;
    private final ApprovalTaskRepository taskRepository;
    private final EvidenceAccessService evidence;
    private final JdbcEvidenceRepository evidenceRepository;
    private final JdbcEvidenceLookup evidenceLookup;
    private final PrivateEvidenceStorage evidenceStorage;

    public KpiAssessmentController(KpiCurrentActor current, KpiIdentityResolver identities, Clock kpiClock,
                                   KpiGrantAdministrationService grants,
                                   KpiScopeAuthorizationService scope, JdbcKpiCatalog catalog, JdbcIndicatorVersionRepository indicatorRepository, JdbcPerformanceSchemeRepository schemeRepository, JdbcCycleSchemeRepository cycleSchemeRepository,
                                   CycleAdministrationService cycles, IndicatorVersionPublicationService publication,
                                   AssessmentSnapshotReader snapshots, AssessmentSubmissionService submissions,
                                   SubmissionContextReader contexts, ResultQueryService results,
                                   ApprovalTaskService tasks, ApprovalTaskRepository taskRepository,
                                   EvidenceAccessService evidence, JdbcEvidenceRepository evidenceRepository,
                                   JdbcEvidenceLookup evidenceLookup, PrivateEvidenceStorage evidenceStorage) {
        this.current = current; this.identities = identities; this.clock = kpiClock; this.grants = grants;
        this.scope = scope; this.catalog = catalog; this.indicatorRepository = indicatorRepository; this.schemeRepository = schemeRepository; this.cycleSchemeRepository = cycleSchemeRepository; this.cycles = cycles;
        this.publication = publication; this.snapshots = snapshots; this.submissions = submissions;
        this.contexts = contexts; this.results = results; this.tasks = tasks; this.taskRepository = taskRepository;
        this.evidence = evidence; this.evidenceRepository = evidenceRepository;
        this.evidenceLookup = evidenceLookup; this.evidenceStorage = evidenceStorage;
    }

    @GetMapping("/cycles")
    public R<List<CycleView>> cycles() {
        current.requireCurrent();
        return R.ok(catalog.cycles().stream().map(cycle -> new CycleView(cycle.code(), cycle.name(), cycle.startsOn(), cycle.endsOn(), cycle.status())).toList());
    }

    @GetMapping("/me")
    public R<KpiActor> currentActor() { return R.ok(current.requireCurrent()); }

    @PostMapping("/security/roles")
    public R<GrantView> grantRole(@RequestBody RoleGrantInput input) {
        String id = grants.grantRole(current.requireCurrent(), input.userId(), KpiRole.valueOf(input.roleCode()), input.endsAt());
        return R.ok(new GrantView(id));
    }

    @DeleteMapping("/security/roles/{bindingId}")
    public R<Void> revokeRole(@PathVariable String bindingId) {
        grants.revokeRole(current.requireCurrent(), bindingId);
        return R.ok();
    }

    @PostMapping("/security/scopes")
    public R<GrantView> grantScope(@RequestBody ScopeGrantInput input) {
        String id = grants.grantScope(current.requireCurrent(), input.userId(), input.cycleCode(), input.unitId(),
            KpiPermission.valueOf(input.permissionCode()), input.expiresAt());
        return R.ok(new GrantView(id));
    }

    @DeleteMapping("/security/scopes/{grantId}")
    public R<Void> revokeScope(@PathVariable String grantId) {
        grants.revokeScope(current.requireCurrent(), grantId);
        return R.ok();
    }

    @PostMapping("/cycles")
    public R<Void> createCycle(@RequestBody CycleInput input) {
        var actor = current.requireCurrent();
        cycles.create(new CycleAdministrationService.CreateCycleCommand(input.code(), input.name(), input.startsOn(), input.endsOn(), actor.roles()));
        return R.ok();
    }

    @PutMapping("/cycles/{cycleCode}/units")
    public R<Void> replaceUnits(@PathVariable String cycleCode, @RequestBody List<OrganizationUnit> units) {
        cycles.captureUnits(cycleCode, units, current.requireCurrent().roles());
        return R.ok();
    }

    @PostMapping("/cycles/{cycleCode}/lock")
    public R<Void> lockCycle(@PathVariable String cycleCode) {
        cycles.lock(cycleCode, current.requireCurrent().roles());
        return R.ok();
    }

    @GetMapping("/units")
    public R<List<OrganizationUnit>> units(@RequestParam String cycleCode) {
        var actor = current.requireCurrent();
        return R.ok(catalog.units(cycleCode).stream()
            .filter(unit -> scope.canAccess(actor, cycleCode, unit.id(), KpiPermission.SUBMISSION_CREATE)
                || scope.canAccess(actor, cycleCode, unit.id(), KpiPermission.RESULT_VIEW)
                || scope.canAccess(actor, cycleCode, unit.id(), KpiPermission.SUBMISSION_REVIEW))
            .toList());
    }

    @GetMapping("/indicator-versions/{version}")
    public R<IndicatorVersionView> indicatorVersion(@PathVariable String version) {
        current.requireCurrent();
        var published = snapshots.indicatorVersion(version);
        return R.ok(new IndicatorVersionView(published.version(), "weighted-score-v1", 4, published.indicators()));
    }

    @GetMapping("/indicator-versions")
    public R<List<JdbcIndicatorVersionRepository.VersionSummary>> indicatorVersions() {
        current.requireCurrent();
        return R.ok(indicatorRepository.listVersions());
    }

    @GetMapping("/schemes")
    public R<List<JdbcPerformanceSchemeRepository.Scheme>> schemes() { current.requireCurrent(); return R.ok(schemeRepository.list()); }

    @PostMapping("/schemes")
    public R<Void> createScheme(@RequestBody SchemeInput input) {
        var actor = current.requireCurrent();
        schemeRepository.create(input.code(), input.name(), input.category(), actor.userId());
        return R.ok();
    }

    @DeleteMapping("/schemes/{code}")
    public R<Void> deleteScheme(@PathVariable String code) { current.requireCurrent(); schemeRepository.deleteDraft(code); return R.ok(); }

    @PutMapping("/schemes/{code}")
    public R<Void> updateScheme(@PathVariable String code, @RequestBody SchemeInput input) {
        current.requireCurrent(); schemeRepository.updateDraft(code, input.name(), input.category()); return R.ok();
    }

    @PostMapping("/schemes/{code}/publish")
    public R<Void> publishScheme(@PathVariable String code) { current.requireCurrent(); schemeRepository.publishDraft(code); return R.ok(); }

    @GetMapping("/cycles/{code}/schemes")
    public R<List<JdbcCycleSchemeRepository.Binding>> cycleSchemes(@PathVariable String code) { current.requireCurrent(); return R.ok(cycleSchemeRepository.list(code)); }

    @PostMapping("/cycles/{code}/schemes")
    public R<Void> bindCycleScheme(@PathVariable String code, @RequestBody CycleSchemeInput input) { var actor=current.requireCurrent(); cycleSchemeRepository.bind(code,input.category(),input.schemeCode(),input.indicatorVersion(),actor.userId()); return R.ok(); }

    @PostMapping("/cycles/{code}/schemes/validate")
    public R<List<String>> validateCycleSchemes(@PathVariable String code) { current.requireCurrent(); return R.ok(cycleSchemeRepository.validate(code)); }

    @GetMapping("/schemes/{code}/indicators")
    public R<List<JdbcPerformanceSchemeRepository.SchemeIndicator>> schemeIndicators(@PathVariable String code) { current.requireCurrent(); return R.ok(schemeRepository.indicators(code)); }

    @PostMapping("/schemes/{code}/indicators")
    public R<Void> addSchemeIndicator(@PathVariable String code, @RequestBody SchemeIndicatorInput input) { current.requireCurrent(); schemeRepository.addIndicator(code, input.code(), input.name(), input.weight(), input.maximumScore()); return R.ok(); }

    @PutMapping("/schemes/{code}/indicators/{indicatorCode}")
    public R<Void> updateSchemeIndicator(@PathVariable String code, @PathVariable String indicatorCode, @RequestBody SchemeIndicatorInput input) { current.requireCurrent(); schemeRepository.updateIndicator(code, indicatorCode, input.name(), input.weight(), input.maximumScore()); return R.ok(); }

    @DeleteMapping("/schemes/{code}/indicators/{indicatorCode}")
    public R<Void> deleteSchemeIndicator(@PathVariable String code, @PathVariable String indicatorCode) { current.requireCurrent(); schemeRepository.deleteIndicator(code, indicatorCode); return R.ok(); }

    @PostMapping("/indicator-versions")
    public R<Void> publishIndicatorVersion(@RequestBody IndicatorVersionInput input) {
        var actor = current.requireCurrent();
        publication.publish(new IndicatorVersion(input.version(), input.indicators()), input.formulaVersion(), input.scoreScale(), actor.roles());
        return R.ok();
    }

    @PostMapping("/submissions/preview")
    public R<ScorePreviewView> preview(@RequestBody ScoreInput input) {
        var actor = current.requireCurrent();
        scope.require(actor, input.cycleCode(), input.unitId(), KpiPermission.SUBMISSION_CREATE);
        var score = submissions.preview(command(input, actor.userId(), actor.roles()));
        return R.ok(new ScorePreviewView(score.totalScore(), score.details().stream()
            .map(detail -> new ScoreDetailView(detail.indicatorCode(), detail.rawScore(), detail.weightedScore())).toList()));
    }

    @PostMapping("/submissions")
    public R<SubmissionView> save(@RequestBody ScoreInput input) {
        var actor = current.requireCurrent();
        scope.require(actor, input.cycleCode(), input.unitId(), KpiPermission.SUBMISSION_CREATE);
        var command = command(input, actor.userId(), actor.roles());
        var score = submissions.preview(command);
        String id = submissions.saveDraft(command);
        return R.ok(new SubmissionView(id, SubmissionState.DRAFT, score.totalScore()));
    }

    @PostMapping("/submissions/{submissionId}/transitions")
    public R<Void> transition(@PathVariable String submissionId, @RequestBody TransitionInput input) {
        var actor = current.requireCurrent();
        var context = contexts.context(submissionId);
        SubmissionState target = SubmissionState.valueOf(input.target());
        KpiPermission permission = switch (target) {
            case SUBMITTED, VOID -> KpiPermission.SUBMISSION_CREATE;
            case RETURNED -> KpiPermission.SUBMISSION_REVIEW;
            case CONFIRMED -> KpiPermission.SUBMISSION_CONFIRM;
            case ARCHIVED -> KpiPermission.SUBMISSION_ARCHIVE;
            case DRAFT -> throw new IllegalArgumentException("cannot transition into draft");
        };
        scope.require(actor, context.cycleCode(), context.unitId(), permission);
        submissions.transition(submissionId, target, actor.userId(), actor.roles());
        return R.ok();
    }

    @GetMapping("/results")
    public R<List<ResultQueryService.RankedResult>> results(@RequestParam String cycleCode, @RequestParam(required = false) String category) {
        var actor = current.requireCurrent();
        return R.ok(results.list(cycleCode, category, actor.roles(),
            row -> scope.canAccess(actor, cycleCode, row.unitId(), KpiPermission.RESULT_VIEW)));
    }

    @GetMapping("/tasks")
    public R<List<ApprovalTaskRepository.Task>> pendingTasks() {
        var actor = current.requireCurrent();
        return R.ok(tasks.pending(actor.userId(), actor.roles()).stream().filter(task -> {
            var context = contexts.context(task.submissionId());
            return scope.canAccess(actor, context.cycleCode(), context.unitId(), KpiPermission.SUBMISSION_REVIEW);
        }).toList());
    }

    @PostMapping("/tasks")
    public R<TaskView> assignReview(@RequestBody TaskAssignmentInput input) {
        var actor = current.requireCurrent();
        var context = contexts.context(input.submissionId());
        scope.require(actor, context.cycleCode(), context.unitId(), KpiPermission.TASK_ASSIGN);
        if (input.assigneeId() == null || input.assigneeId().isBlank()
            || !scope.canAccess(identities.resolve(input.assigneeId(), clock.instant()),
                context.cycleCode(), context.unitId(), KpiPermission.SUBMISSION_REVIEW)) {
            throw new IllegalArgumentException("assignee lacks review permission for this assessment unit");
        }
        String id = tasks.assignReview(input.submissionId(), input.assigneeId(), actor.roles());
        return R.ok(new TaskView(id, input.submissionId(), "PENDING"));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public R<Void> completeTask(@PathVariable String taskId, @RequestBody TaskDecisionInput input) {
        var actor = current.requireCurrent();
        var task = taskRepository.task(taskId);
        var context = contexts.context(task.submissionId());
        scope.require(actor, context.cycleCode(), context.unitId(), KpiPermission.SUBMISSION_REVIEW);
        if (input.approved()) scope.require(actor, context.cycleCode(), context.unitId(), KpiPermission.SUBMISSION_CONFIRM);
        tasks.complete(taskId, input.approved(), input.comment(), actor.userId(), actor.roles());
        return R.ok();
    }

    @PostMapping(value = "/submissions/{submissionId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<EvidenceView> uploadEvidence(@PathVariable String submissionId, @RequestParam String indicatorCode,
                                          @RequestPart("file") MultipartFile file) {
        var actor = current.requireCurrent();
        var context = contexts.context(submissionId);
        scope.require(actor, context.cycleCode(), context.unitId(), KpiPermission.SUBMISSION_CREATE);
        var state = submissions.stateOf(submissionId);
        if (state != SubmissionState.DRAFT && state != SubmissionState.RETURNED) throw new IllegalStateException("materials can be added only to an editable submission");
        String original = file.getOriginalFilename() == null ? "material" : file.getOriginalFilename().replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1);
        if (original.isBlank() || original.length() > 512) throw new IllegalArgumentException("invalid material filename");
        var stored = evidenceStorage.store(file);
        String evidenceId = null;
        try {
            evidenceId = evidence.register(new EvidenceRepository.EvidenceMetadata(stored.storageKey(), original,
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(), stored.byteSize(), stored.sha256()),
                actor.userId(), actor.roles());
            evidence.attach(submissionId, indicatorCode, evidenceId, actor.userId(), actor.roles());
            return R.ok(new EvidenceView(evidenceId, original, stored.byteSize(), stored.sha256()));
        } catch (RuntimeException error) {
            if (evidenceId != null) evidenceRepository.removeUnattached(evidenceId, actor.userId());
            evidenceStorage.discard(stored.storageKey());
            throw error;
        }
    }

    @GetMapping("/evidence/{evidenceId}/download")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable String evidenceId) {
        var actor = current.requireCurrent();
        var metadata = evidenceLookup.accessRows(evidenceId).stream()
            .filter(row -> scope.canAccess(actor, row.cycleCode(), row.unitId(), KpiPermission.EVIDENCE_READ))
            .findFirst().orElseThrow(() -> new SecurityException("material is outside the authorized assessment scope"));
        byte[] content = evidenceStorage.read(metadata.storageKey());
        evidence.access(evidenceId, actor.userId(), EvidenceRepository.AccessAction.DOWNLOAD, UUID.randomUUID().toString(), actor.roles());
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(metadata.filename(), StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.APPLICATION_OCTET_STREAM).body(content);
    }

    private static AssessmentSubmissionService.ScorePreviewCommand command(ScoreInput input, String actorId, java.util.Set<cn.edu.kpi.domain.security.KpiRole> roles) {
        return new AssessmentSubmissionService.ScorePreviewCommand(input.cycleCode(), input.unitId(), input.indicatorVersion(), actorId, roles,
            input.scores().stream().map(item -> new AssessmentSubmissionService.ScoreInput(item.indicatorCode(), item.rawScore(), List.of())).toList());
    }

    public record CycleInput(String code, String name, LocalDate startsOn, LocalDate endsOn) { }
    public record CycleView(String code, String name, LocalDate startsOn, LocalDate endsOn, AssessmentCycle.Status state) { }
    public record IndicatorVersionInput(String version, String formulaVersion, int scoreScale, List<IndicatorVersion.Indicator> indicators) { }
    public record IndicatorVersionView(String version, String formulaVersion, int scoreScale, List<IndicatorVersion.Indicator> indicators) { }
    public record SchemeInput(String code, String name, String category) { }
    public record SchemeIndicatorInput(String code, String name, BigDecimal weight, BigDecimal maximumScore) { }
    public record CycleSchemeInput(String category, String schemeCode, String indicatorVersion) { }
    public record ScoreInput(String cycleCode, String unitId, String indicatorVersion, List<ScoreItemInput> scores) { }
    public record ScoreItemInput(String indicatorCode, BigDecimal rawScore, List<String> evidenceIds) { }
    public record ScorePreviewView(BigDecimal totalScore, List<ScoreDetailView> details) { }
    public record ScoreDetailView(String indicatorCode, BigDecimal rawScore, BigDecimal weightedScore) { }
    public record SubmissionView(String id, SubmissionState state, BigDecimal totalScore) { }
    public record TransitionInput(String target, String comment) { }
    public record TaskAssignmentInput(String submissionId, String assigneeId) { }
    public record TaskDecisionInput(boolean approved, String comment) { }
    public record TaskView(String id, String submissionId, String state) { }
    public record EvidenceView(String id, String filename, long byteSize, String sha256) { }
    public record RoleGrantInput(String userId, String roleCode, Instant endsAt) { }
    public record ScopeGrantInput(String userId, String cycleCode, String unitId, String permissionCode, Instant expiresAt) { }
    public record GrantView(String id) { }
}
