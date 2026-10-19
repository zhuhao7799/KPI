package cn.edu.kpi.application.cycle;

import cn.edu.kpi.domain.model.AssessmentCycle;

import java.util.Optional;

/** Persistence port for a configured assessment period. */
public interface AssessmentCycleRepository {
    Optional<AssessmentCycle> findByCode(String cycleCode);

    void insert(AssessmentCycle cycle);

    void updateStatus(String cycleCode, AssessmentCycle.Status target);
}
