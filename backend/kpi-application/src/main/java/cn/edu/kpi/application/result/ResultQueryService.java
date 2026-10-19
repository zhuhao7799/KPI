package cn.edu.kpi.application.result;

import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Ranking is a read projection; it never recalculates historical scores. */
public final class ResultQueryService {
    private final ResultQueryRepository repository;
    public ResultQueryService(ResultQueryRepository repository) { this.repository = Objects.requireNonNull(repository); }

    public List<RankedResult> list(String cycleCode, String category, Set<KpiRole> roles) {
        return list(cycleCode, category, roles, row -> true);
    }

    /** Filter unauthorized units before ranking so hidden rows cannot affect visible ranks. */
    public List<RankedResult> list(String cycleCode, String category, Set<KpiRole> roles, Predicate<ResultQueryRepository.ResultRow> visible) {
        KpiAuthorization.require(roles, KpiPermission.RESULT_VIEW);
        var rows = repository.listFinalized(cycleCode).stream()
            .filter(row -> category == null || category.isBlank() || category.equals(row.unitCategory()))
            .filter(Objects.requireNonNull(visible))
            .sorted(Comparator.comparing(ResultQueryRepository.ResultRow::totalScore).reversed().thenComparing(ResultQueryRepository.ResultRow::unitName))
            .toList();
        var ranked = new java.util.ArrayList<RankedResult>();
        int rank = 0; BigDecimal previous = null;
        for (int index = 0; index < rows.size(); index++) {
            var row = rows.get(index);
            if (previous == null || row.totalScore().compareTo(previous) != 0) rank = index + 1;
            ranked.add(new RankedResult(rank, row.submissionId(), row.unitId(), row.unitName(), row.unitCategory(), row.totalScore()));
            previous = row.totalScore();
        }
        return List.copyOf(ranked);
    }

    public record RankedResult(int rank, String submissionId, String unitId, String unitName, String unitCategory, BigDecimal totalScore) { }
}
