package cn.edu.kpi.domain.example;

import cn.edu.kpi.domain.model.AssessmentCycle;
import cn.edu.kpi.domain.model.IndicatorVersion;
import cn.edu.kpi.domain.model.OrganizationUnit;
import cn.edu.kpi.domain.score.ScoreCalculator;
import cn.edu.kpi.domain.submission.SubmissionLifecycle;
import cn.edu.kpi.domain.submission.SubmissionState;
import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Executable invariant checks without an external test framework. */
public final class CoreInvariantTest {
    private static final OrganizationUnit UNIT = new OrganizationUnit("u-1", "虚构单位", "COLLEGE", null);
    private static final IndicatorVersion VERSION = new IndicatorVersion("v-1", List.of(
        new IndicatorVersion.Indicator("a", "指标 A", new BigDecimal("50"), new BigDecimal("100")),
        new IndicatorVersion.Indicator("b", "指标 B", new BigDecimal("50"), new BigDecimal("100"))));

    public static void main(String[] args) {
        rejectsInvalidWeightTotal();
        rejectsActiveCycle();
        rejectsOutOfRangeScore();
        rejectsDuplicateScore();
        allowsOnlyDefinedSubmissionTransitions();
        preventsArchivedSubmissionMutation();
        enforcesRolePermissions();
        System.out.println("invariants=7 passed");
    }

    private static void rejectsInvalidWeightTotal() {
        expectFailure(() -> new IndicatorVersion("bad", List.of(
            new IndicatorVersion.Indicator("a", "A", new BigDecimal("99"), BigDecimal.ONE))));
    }

    private static void rejectsActiveCycle() {
        expectFailure(() -> calculator().calculate(cycle(AssessmentCycle.Status.ACTIVE), UNIT, VERSION, validInputs(), "tester", Instant.EPOCH));
    }

    private static void rejectsOutOfRangeScore() {
        expectFailure(() -> calculator().calculate(cycle(AssessmentCycle.Status.LOCKED), UNIT, VERSION, List.of(
            input("a", "101"), input("b", "80")), "tester", Instant.EPOCH));
    }

    private static void rejectsDuplicateScore() {
        expectFailure(() -> calculator().calculate(cycle(AssessmentCycle.Status.LOCKED), UNIT, VERSION, List.of(
            input("a", "80"), input("a", "70"), input("b", "80")), "tester", Instant.EPOCH));
    }

    private static void allowsOnlyDefinedSubmissionTransitions() {
        if (SubmissionLifecycle.transition(SubmissionState.DRAFT, SubmissionState.SUBMITTED) != SubmissionState.SUBMITTED) {
            throw new AssertionError("valid transition was not applied");
        }
        expectFailure(() -> SubmissionLifecycle.transition(SubmissionState.DRAFT, SubmissionState.ARCHIVED));
    }

    private static void preventsArchivedSubmissionMutation() {
        if (!SubmissionLifecycle.isTerminal(SubmissionState.ARCHIVED)) throw new AssertionError("archived should be terminal");
        expectFailure(() -> SubmissionLifecycle.transition(SubmissionState.ARCHIVED, SubmissionState.RETURNED));
    }

    private static void enforcesRolePermissions() {
        if (!KpiAuthorization.isAllowed(java.util.Set.of(KpiRole.UNIT_EDITOR), KpiPermission.SUBMISSION_CREATE)) {
            throw new AssertionError("unit editor should create a submission");
        }
        if (KpiAuthorization.isAllowed(java.util.Set.of(KpiRole.RESULT_VIEWER), KpiPermission.SUBMISSION_CONFIRM)) {
            throw new AssertionError("result viewer should not confirm a submission");
        }
        expectSecurityFailure(() -> KpiAuthorization.require(java.util.Set.of(KpiRole.RESULT_VIEWER), KpiPermission.EVIDENCE_READ));
    }

    private static AssessmentCycle cycle(AssessmentCycle.Status status) {
        return new AssessmentCycle("2026", "虚构年度", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), status);
    }

    private static ScoreCalculator calculator() { return new ScoreCalculator(); }
    private static List<ScoreCalculator.ScoreInput> validInputs() { return List.of(input("a", "80"), input("b", "80")); }
    private static ScoreCalculator.ScoreInput input(String code, String score) { return new ScoreCalculator.ScoreInput(code, new BigDecimal(score), List.of()); }

    private static void expectFailure(Runnable operation) {
        try { operation.run(); }
        catch (IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("expected validation failure");
    }

    private static void expectSecurityFailure(Runnable operation) {
        try { operation.run(); }
        catch (SecurityException expected) { return; }
        throw new AssertionError("expected authorization failure");
    }
}
