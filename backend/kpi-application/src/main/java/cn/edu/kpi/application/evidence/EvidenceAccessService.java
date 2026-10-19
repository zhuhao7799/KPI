package cn.edu.kpi.application.evidence;

import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.time.Clock;
import java.util.Objects;
import java.util.Set;

/** Issues no public URL: each private-object access is authorized and audited. */
public final class EvidenceAccessService {
    private final EvidenceRepository repository;
    private final Clock clock;
    public EvidenceAccessService(EvidenceRepository repository, Clock clock) { this.repository = Objects.requireNonNull(repository); this.clock = Objects.requireNonNull(clock); }

    public String register(EvidenceRepository.EvidenceMetadata metadata, String actorId, Set<KpiRole> roles) {
        KpiAuthorization.require(roles, KpiPermission.SUBMISSION_CREATE);
        if (metadata.byteSize() <= 0 || metadata.storageKey().isBlank() || metadata.sha256().length() != 64) throw new IllegalArgumentException("invalid private evidence metadata");
        return repository.register(metadata, actorId, clock.instant());
    }
    public void attach(String submissionId, String indicatorCode, String evidenceId, String actorId, Set<KpiRole> roles) {
        KpiAuthorization.require(roles, KpiPermission.SUBMISSION_CREATE);
        repository.attach(submissionId, indicatorCode, evidenceId, actorId, clock.instant());
    }
    public void access(String evidenceId, String actorId, EvidenceRepository.AccessAction action, String requestId, Set<KpiRole> roles) {
        KpiAuthorization.require(roles, KpiPermission.EVIDENCE_READ);
        repository.auditAccess(evidenceId, actorId, action, requestId, clock.instant());
    }
}
