package cn.edu.kpi.application.assessment;

import cn.edu.kpi.domain.model.EvidenceReference;
import cn.edu.kpi.domain.score.ScoreCalculator;
import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;
import cn.edu.kpi.domain.submission.SubmissionLifecycle;
import cn.edu.kpi.domain.submission.SubmissionState;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Application boundary between authenticated APIs and deterministic KPI rules. */
public final class AssessmentSubmissionService {
    private final AssessmentSnapshotReader snapshots;
    private final SubmissionRepository submissions;
    private final Clock clock;
    private final ScoreCalculator calculator = new ScoreCalculator();

    public AssessmentSubmissionService(AssessmentSnapshotReader snapshots, SubmissionRepository submissions, Clock clock) {
        this.snapshots = Objects.requireNonNull(snapshots);
        this.submissions = Objects.requireNonNull(submissions);
        this.clock = Objects.requireNonNull(clock);
    }

    public ScoreCalculator.ScoreResult preview(ScorePreviewCommand command) {
        KpiAuthorization.require(command.roles(), KpiPermission.SUBMISSION_CREATE);
        return calculator.calculate(
            snapshots.cycle(command.cycleCode()),
            snapshots.unit(command.cycleCode(), command.unitId()),
            snapshots.indicatorVersion(command.indicatorVersion()),
            command.scores().stream().map(ScoreInput::toDomain).toList(),
            command.actorId(), clock.instant());
    }

    public String saveDraft(ScorePreviewCommand command) {
        return submissions.createDraft(preview(command), clock.instant());
    }

    public SubmissionState stateOf(String submissionId) { return submissions.stateOf(submissionId); }

    public void transition(String submissionId, SubmissionState target, String actorId, Set<KpiRole> roles) {
        SubmissionState current = validateTransition(submissionId, target, roles);
        submissions.changeState(submissionId, current, target, actorId, clock.instant());
    }

    public SubmissionState validateTransition(String submissionId, SubmissionState target, Set<KpiRole> roles) {
        KpiAuthorization.require(roles, permissionFor(target));
        SubmissionState current = submissions.stateOf(submissionId);
        SubmissionLifecycle.transition(current, target);
        return current;
    }

    private KpiPermission permissionFor(SubmissionState target) {
        return switch (target) {
            case SUBMITTED -> KpiPermission.SUBMISSION_CREATE;
            case RETURNED -> KpiPermission.SUBMISSION_REVIEW;
            case CONFIRMED -> KpiPermission.SUBMISSION_CONFIRM;
            case ARCHIVED -> KpiPermission.SUBMISSION_ARCHIVE;
            case VOID -> KpiPermission.SUBMISSION_CREATE;
            case DRAFT -> throw new IllegalArgumentException("cannot transition into draft");
        };
    }

    public record ScorePreviewCommand(String cycleCode, String unitId, String indicatorVersion, String actorId,
                                      Set<KpiRole> roles, List<ScoreInput> scores) {
        public ScorePreviewCommand { roles = Set.copyOf(roles); scores = List.copyOf(scores); }
    }

    public record ScoreInput(String indicatorCode, BigDecimal rawScore, List<EvidenceReference> evidence) {
        public ScoreInput { evidence = List.copyOf(evidence); }
        ScoreCalculator.ScoreInput toDomain() { return new ScoreCalculator.ScoreInput(indicatorCode, rawScore, evidence); }
    }
}
