package cn.edu.kpi.application.result;

import cn.edu.kpi.domain.security.KpiRole;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** Hidden units must not change the ranks returned to a scoped viewer. */
public final class ResultScopeRankingTest {
    public static void main(String[] args) {
        ResultQueryRepository repository = cycle -> List.of(
            new ResultQueryRepository.ResultRow("hidden", "unit-x", "其他学院", "COLLEGE", new BigDecimal("99")),
            new ResultQueryRepository.ResultRow("a", "unit-a", "甲学院", "COLLEGE", new BigDecimal("92")),
            new ResultQueryRepository.ResultRow("b", "unit-b", "乙学院", "COLLEGE", new BigDecimal("92")),
            new ResultQueryRepository.ResultRow("c", "unit-c", "丙学院", "COLLEGE", new BigDecimal("88")));
        var rows = new ResultQueryService(repository).list("2026", "COLLEGE", Set.of(KpiRole.RESULT_VIEWER),
            row -> !row.unitId().equals("unit-x"));
        if (rows.size() != 3 || rows.get(0).rank() != 1 || rows.get(1).rank() != 1 || rows.get(2).rank() != 3) {
            throw new AssertionError("scoped ranking leaked a hidden row or broke tied ranks: " + rows);
        }
        System.out.println("result-scope-ranking=passed");
    }
}
