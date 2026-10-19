package cn.edu.kpi.application.evidence;

import java.time.Instant;

/** Metadata only; object bytes remain in a private object store. */
public interface EvidenceRepository {
    String register(EvidenceMetadata metadata, String actorId, Instant occurredAt);
    void attach(String submissionId, String indicatorCode, String evidenceId, String actorId, Instant occurredAt);
    void auditAccess(String evidenceId, String actorId, AccessAction action, String requestId, Instant occurredAt);

    record EvidenceMetadata(String storageKey, String filename, String mediaType, long byteSize, String sha256) { }
    enum AccessAction { VIEW, DOWNLOAD }
}
