import type { AxiosPromise } from '@/utils/api-types';
import request from '@/utils/request';
import type { AssessmentCycleVO, ApprovalTaskVO, EvidenceAttachmentInput, EvidenceAttachmentVO, EvidenceRegisterInput, EvidenceRegisterVO, EvidenceUploadVO, IndicatorVersionInput, IndicatorVersionSummary, KpiActorVO, OrganizationUnitVO, RankedResultVO, ScorePreviewRequest, ScorePreviewVO, SubmissionVO } from './types';

export const listCycles = (): AxiosPromise<AssessmentCycleVO[]> => request({ url: '/kpi/cycles', method: 'get' });
export const getCurrentKpiActor = (): AxiosPromise<KpiActorVO> => request({ url: '/kpi/me', method: 'get' });
export const createCycle = (data: AssessmentCycleVO) => request({ url: '/kpi/cycles', method: 'post', data });
export const replaceCycleUnits = (cycleCode: string, data: OrganizationUnitVO[]) => request({ url: `/kpi/cycles/${cycleCode}/units`, method: 'put', data });
export const lockCycle = (cycleCode: string) => request({ url: `/kpi/cycles/${cycleCode}/lock`, method: 'post' });
export interface CycleSchemeVO { category: string; schemeCode: string; schemeName: string; indicatorVersion: string; status: string; }
export const listCycleSchemes = (code: string): AxiosPromise<CycleSchemeVO[]> => request({ url: `/kpi/cycles/${code}/schemes`, method: 'get' });
export const bindCycleScheme = (code: string, data: { category: string; schemeCode: string; indicatorVersion: string }) => request({ url: `/kpi/cycles/${code}/schemes`, method: 'post', data });
export const validateCycleSchemes = (code: string): AxiosPromise<string[]> => request({ url: `/kpi/cycles/${code}/schemes/validate`, method: 'post' });
export const listOrganizationUnits = (cycleCode: string): AxiosPromise<OrganizationUnitVO[]> => request({ url: '/kpi/units', method: 'get', params: { cycleCode } });
export const getIndicatorVersion = (version: string): AxiosPromise<IndicatorVersionInput> => request({ url: `/kpi/indicator-versions/${version}`, method: 'get' });
export const listIndicatorVersions = (): AxiosPromise<IndicatorVersionSummary[]> => request({ url: '/kpi/indicator-versions', method: 'get' });
export interface PerformanceSchemeVO { code: string; name: string; category: string; status: string; formulaVersion: string; indicatorVersion: string; }
export const listPerformanceSchemes = (): AxiosPromise<PerformanceSchemeVO[]> => request({ url: '/kpi/schemes', method: 'get' });
export const createPerformanceScheme = (data: { code: string; name: string; category: string; indicatorVersion: string }) => request({ url: '/kpi/schemes', method: 'post', data });
export const deletePerformanceScheme = (code: string) => request({ url: `/kpi/schemes/${code}`, method: 'delete' });
export const updatePerformanceScheme = (code: string, data: { name: string; category: string; indicatorVersion: string }) => request({ url: `/kpi/schemes/${code}`, method: 'put', data });
export const publishPerformanceScheme = (code: string) => request({ url: `/kpi/schemes/${code}/publish`, method: 'post' });
export interface SchemeIndicatorVO { code: string; name: string; weight: number; maximumScore: number; }
export const listSchemeIndicators = (code: string): AxiosPromise<SchemeIndicatorVO[]> => request({ url: `/kpi/schemes/${code}/indicators`, method: 'get' });
export const addSchemeIndicator = (code: string, data: SchemeIndicatorVO) => request({ url: `/kpi/schemes/${code}/indicators`, method: 'post', data });
export const deleteSchemeIndicator = (scheme: string, indicator: string) => request({ url: `/kpi/schemes/${scheme}/indicators/${indicator}`, method: 'delete' });
export const updateSchemeIndicator = (scheme: string, indicator: string, data: { weight: number }) => request({ url: `/kpi/schemes/${scheme}/indicators/${indicator}`, method: 'put', data });
export const publishIndicatorVersion = (data: IndicatorVersionInput) => request({ url: '/kpi/indicator-versions', method: 'post', data });
export const previewScore = (data: ScorePreviewRequest): AxiosPromise<ScorePreviewVO> => request({ url: '/kpi/submissions/preview', method: 'post', data });
export const saveSubmission = (data: ScorePreviewRequest): AxiosPromise<SubmissionVO> => request({ url: '/kpi/submissions', method: 'post', data });
export const transitionSubmission = (submissionId: string, target: string, comment?: string) => request({ url: `/kpi/submissions/${submissionId}/transitions`, method: 'post', data: { target, comment } });
export const archiveSubmission = (submissionId: string) => transitionSubmission(submissionId, 'ARCHIVED', '结果归档');
export const registerEvidence = (data: EvidenceRegisterInput): AxiosPromise<EvidenceRegisterVO> => request({ url: '/kpi/evidence', method: 'post', data });
export const attachEvidence = (evidenceId: string, data: EvidenceAttachmentInput): AxiosPromise<EvidenceAttachmentVO> => request({ url: `/kpi/evidence/${evidenceId}/attachments`, method: 'post', data });
export const uploadSubmissionEvidence = (submissionId: string, indicatorCode: string, file: File): AxiosPromise<EvidenceUploadVO> => {
  const data = new FormData();
  data.append('indicatorCode', indicatorCode);
  data.append('file', file);
  return request({ url: `/kpi/submissions/${submissionId}/evidence`, method: 'post', data });
};
export const listTasks = (): AxiosPromise<ApprovalTaskVO[]> => request({ url: '/kpi/tasks', method: 'get' });
export const assignReview = (submissionId: string, assigneeId: string) => request({ url: '/kpi/tasks', method: 'post', data: { submissionId, assigneeId } });
export const completeTask = (taskId: string, approved: boolean, comment?: string) => request({ url: `/kpi/tasks/${taskId}/complete`, method: 'post', data: { approved, comment } });
export const listResults = (cycleCode: string, category?: string): AxiosPromise<RankedResultVO[]> => request({ url: '/kpi/results', method: 'get', params: { cycleCode, category } });
