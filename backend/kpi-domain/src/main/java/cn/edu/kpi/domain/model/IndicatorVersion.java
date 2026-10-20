package cn.edu.kpi.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Immutable indicator configuration used as calculation input. */
public record IndicatorVersion(String version, List<Indicator> indicators) {
    public IndicatorVersion {
        AssessmentCycle.requireText(version, "indicator version");
        indicators = List.copyOf(Objects.requireNonNull(indicators, "indicators"));
    }

    public record Indicator(String code, String name, BigDecimal weight, BigDecimal maximumScore,
                            String category, String indicatorType, String measurementUnit,
                            String scoringMethod, boolean evidenceRequired, String description) {
        public Indicator(String code, String name, BigDecimal weight, BigDecimal maximumScore) {
            this(code, name, weight, maximumScore, "通用指标", "定量", "分", "加权评分", false, "");
        }
        public Indicator {
            AssessmentCycle.requireText(code, "indicator code");
            AssessmentCycle.requireText(name, "indicator name");
            Objects.requireNonNull(weight, "weight");
            Objects.requireNonNull(maximumScore, "maximumScore");
            if (weight.signum() <= 0 || maximumScore.signum() <= 0) {
                throw new IllegalArgumentException("indicator weight and maximum score must be positive");
            }
            category = category == null || category.isBlank() ? "通用指标" : category;
            indicatorType = indicatorType == null || indicatorType.isBlank() ? "定量" : indicatorType;
            measurementUnit = measurementUnit == null || measurementUnit.isBlank() ? "分" : measurementUnit;
            scoringMethod = scoringMethod == null || scoringMethod.isBlank() ? "加权评分" : scoringMethod;
            description = description == null ? "" : description;
        }
    }
}
