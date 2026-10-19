package cn.edu.kpi.application.assessment;

import cn.edu.kpi.domain.model.AssessmentCycle;
import cn.edu.kpi.domain.model.IndicatorVersion;
import cn.edu.kpi.domain.model.OrganizationUnit;

/** Read port implemented by the persistence adapter, never by a controller. */
public interface AssessmentSnapshotReader {
    AssessmentCycle cycle(String cycleCode);

    OrganizationUnit unit(String cycleCode, String unitId);

    IndicatorVersion indicatorVersion(String versionCode);
}
