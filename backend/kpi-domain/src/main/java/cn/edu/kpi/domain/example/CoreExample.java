package cn.edu.kpi.domain.example;

import cn.edu.kpi.domain.model.AssessmentCycle;
import cn.edu.kpi.domain.model.EvidenceReference;
import cn.edu.kpi.domain.model.IndicatorVersion;
import cn.edu.kpi.domain.model.OrganizationUnit;
import cn.edu.kpi.domain.score.ScoreCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Synthetic executable example; it contains no real school policy or evidence. */
public final class CoreExample {
    public static void main(String[] args) {
        var cycle = new AssessmentCycle("2026", "2026 年度", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), AssessmentCycle.Status.LOCKED);
        var unit = new OrganizationUnit("unit-demo-001", "示例学院", "COLLEGE", null);
        var version = new IndicatorVersion("2026-v1", List.of(
            new IndicatorVersion.Indicator("goal", "年度目标完成", new BigDecimal("60"), new BigDecimal("100")),
            new IndicatorVersion.Indicator("service", "服务保障", new BigDecimal("40"), new BigDecimal("100"))));
        var evidence = new EvidenceReference("evidence-demo-001", "sha256:demo-only");
        var result = new ScoreCalculator().calculate(cycle, unit, version, List.of(
            new ScoreCalculator.ScoreInput("goal", new BigDecimal("92"), List.of(evidence)),
            new ScoreCalculator.ScoreInput("service", new BigDecimal("88"), List.of(evidence))), "reviewer-demo", Instant.parse("2026-10-18T00:00:00Z"));
        System.out.println("cycle=" + result.cycleCode());
        System.out.println("unit=" + result.unitId());
        System.out.println("indicatorVersion=" + result.indicatorVersion());
        System.out.println("totalScore=" + result.totalScore());
        System.out.println("detailCount=" + result.details().size());
    }
}
