package cn.edu.kpi.application.cycle;

import cn.edu.kpi.domain.model.OrganizationUnit;

import java.time.Instant;
import java.util.List;

/** The adapter must reject duplicates by the database's cycle/unit unique key. */
public interface OrganizationUnitSnapshotRepository {
    void replaceForDraftCycle(String cycleCode, List<OrganizationUnit> units, Instant capturedAt);
}
