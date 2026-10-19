package cn.edu.kpi.domain.model;

/** A snapshot of the organizational unit being assessed. */
public record OrganizationUnit(String id, String name, String category, String parentId) {
    public OrganizationUnit {
        AssessmentCycle.requireText(id, "organization id");
        AssessmentCycle.requireText(name, "organization name");
        AssessmentCycle.requireText(category, "organization category");
    }
}
