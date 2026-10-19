package cn.edu.kpi.domain.score;

import cn.edu.kpi.domain.model.AssessmentCycle;
import cn.edu.kpi.domain.model.EvidenceReference;
import cn.edu.kpi.domain.model.IndicatorVersion;
import cn.edu.kpi.domain.model.OrganizationUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Deterministic calculation over frozen cycle, unit and indicator inputs. */
public final class ScoreCalculator {
    private static final int SCALE = 4;

    public ScoreResult calculate(AssessmentCycle cycle, OrganizationUnit unit, IndicatorVersion version,
                                 List<ScoreInput> inputs, String actorId, Instant calculatedAt) {
        Objects.requireNonNull(cycle); Objects.requireNonNull(unit); Objects.requireNonNull(version);
        Objects.requireNonNull(inputs); Objects.requireNonNull(actorId); Objects.requireNonNull(calculatedAt);
        if (cycle.status() != AssessmentCycle.Status.LOCKED && cycle.status() != AssessmentCycle.Status.ARCHIVED) {
            throw new IllegalStateException("scores can be calculated only for a locked cycle");
        }
        Map<String, ScoreInput> byCode = new LinkedHashMap<>();
        for (ScoreInput input : inputs) {
            if (byCode.put(input.indicatorCode(), input) != null) throw new IllegalArgumentException("duplicate score: " + input.indicatorCode());
        }
        Set<String> expected = version.indicators().stream().map(IndicatorVersion.Indicator::code).collect(Collectors.toSet());
        if (!byCode.keySet().equals(expected)) throw new IllegalArgumentException("a score is required for every frozen indicator");

        var details = version.indicators().stream().map(indicator -> {
            var input = byCode.get(indicator.code());
            if (input.rawScore().signum() < 0 || input.rawScore().compareTo(indicator.maximumScore()) > 0) {
                throw new IllegalArgumentException("score outside range for " + indicator.code());
            }
            var weighted = input.rawScore().divide(indicator.maximumScore(), SCALE + 4, RoundingMode.HALF_UP)
                .multiply(indicator.weight()).setScale(SCALE, RoundingMode.HALF_UP);
            return new ScoreDetail(indicator.code(), indicator.name(), input.rawScore(), indicator.maximumScore(), indicator.weight(), weighted, input.evidence());
        }).toList();
        var total = details.stream().map(ScoreDetail::weightedScore).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(SCALE, RoundingMode.HALF_UP);
        return new ScoreResult(cycle.code(), unit.id(), version.version(), total, details, actorId, calculatedAt);
    }

    public record ScoreInput(String indicatorCode, BigDecimal rawScore, List<EvidenceReference> evidence) {
        public ScoreInput { Objects.requireNonNull(indicatorCode); Objects.requireNonNull(rawScore); evidence = List.copyOf(Objects.requireNonNull(evidence)); }
    }
    public record ScoreDetail(String indicatorCode, String indicatorName, BigDecimal rawScore, BigDecimal maximumScore, BigDecimal weight,
                              BigDecimal weightedScore, List<EvidenceReference> evidence) { }
    public record ScoreResult(String cycleCode, String unitId, String indicatorVersion, BigDecimal totalScore,
                              List<ScoreDetail> details, String calculatedBy, Instant calculatedAt) { }
}
