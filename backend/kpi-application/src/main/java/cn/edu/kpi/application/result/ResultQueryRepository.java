package cn.edu.kpi.application.result;

import java.math.BigDecimal;
import java.util.List;

/** Read model contains only confirmed or archived score snapshots. */
public interface ResultQueryRepository {
    List<ResultRow> listFinalized(String cycleCode);

    record ResultRow(String submissionId, String unitId, String unitName, String unitCategory, BigDecimal totalScore) { }
}
