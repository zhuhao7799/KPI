package cn.edu.kpi.domain.model;

/** Reference only: access is authorized by the future evidence service. */
public record EvidenceReference(String objectId, String contentHash) {
    public EvidenceReference {
        AssessmentCycle.requireText(objectId, "evidence object id");
        AssessmentCycle.requireText(contentHash, "evidence hash");
    }
}
