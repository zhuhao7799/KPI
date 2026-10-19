export interface AssessmentCycleVO { code: string; name: string; startsOn: string; endsOn: string; state: 'DRAFT' | 'ACTIVE' | 'LOCKED' | 'ARCHIVED'; }
export interface KpiActorVO { userId: string; roles: Array<'SCHOOL_ADMIN' | 'ASSESSMENT_OFFICE' | 'UNIT_EDITOR' | 'REVIEWER' | 'EVALUATOR' | 'RESULT_VIEWER'>; }
export interface OrganizationUnitVO { id: string; name: string; category: string; parentId?: string; }
export interface IndicatorItemInput { code: string; name: string; weight: number; maximumScore: number; }
export interface IndicatorVersionInput { version: string; formulaVersion: string; scoreScale: number; indicators: IndicatorItemInput[]; }
export interface ScoreInput { indicatorCode: string; rawScore: number; evidenceIds: string[]; }
export interface ScorePreviewRequest { cycleCode: string; unitId: string; indicatorVersion: string; scores: ScoreInput[]; }
export interface ScorePreviewVO { totalScore: number; details: Array<ScoreInput & { weightedScore: number }>; }
export interface SubmissionVO { id: string; state: 'DRAFT' | 'SUBMITTED' | 'RETURNED' | 'CONFIRMED' | 'ARCHIVED' | 'VOID'; totalScore: number; }
export interface EvidenceRegisterInput { storageKey: string; filename: string; mediaType: string; byteSize: number; sha256: string; }
export interface EvidenceRegisterVO { id: string; private: boolean; }
export interface EvidenceAttachmentInput { submissionId: string; indicatorCode: string; }
export interface EvidenceAttachmentVO extends EvidenceAttachmentInput { evidenceId: string; }
export interface EvidenceUploadVO { id: string; filename: string; byteSize: number; sha256: string; }
export interface RankedResultVO { rank: number; submissionId: string; unitId: string; unitName: string; unitCategory: string; totalScore: number; }
export interface ApprovalTaskVO { id: string; submissionId: string; type: 'REVIEW' | 'CONFIRM' | 'ARCHIVE'; state: 'PENDING' | 'COMPLETED' | 'CANCELLED'; }
