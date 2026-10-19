package cn.edu.kpi.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Immutable indicator configuration used as calculation input. */
public record IndicatorVersion(String version, List<Indicator> indicators) {
    public IndicatorVersion {
        AssessmentCycle.requireText(version, "indicator version");
        indicators = List.copyOf(Objects.requireNonNull(indicators, "indicators"));
        if (indicators.isEmpty()) throw new IllegalArgumentException("at least one indicator is required");
        var total = indicators.stream().map(Indicator::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(new BigDecimal("100")) != 0) {
            throw new IllegalArgumentException("indicator weights must total 100, got " + total);
        }
    }

    public record Indicator(String code, String name, BigDecimal weight, BigDecimal maximumScore) {
        public Indicator {
            AssessmentCycle.requireText(code, "indicator code");
            AssessmentCycle.requireText(name, "indicator name");
            Objects.requireNonNull(weight, "weight");
            Objects.requireNonNull(maximumScore, "maximumScore");
            if (weight.signum() <= 0 || maximumScore.signum() <= 0) {
                throw new IllegalArgumentException("indicator weight and maximum score must be positive");
            }
        }
    }
}
