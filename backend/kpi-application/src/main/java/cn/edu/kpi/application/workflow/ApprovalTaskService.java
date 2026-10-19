package cn.edu.kpi.application.workflow;

import cn.edu.kpi.application.assessment.AssessmentSubmissionService;
import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;
import cn.edu.kpi.domain.submission.SubmissionState;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Local, auditable task bridge; WarmFlow will later drive the same actions. */
public final class ApprovalTaskService {
    private final ApprovalTaskRepository tasks; private final AssessmentSubmissionService submissions; private final Clock clock;
    public ApprovalTaskService(ApprovalTaskRepository tasks, AssessmentSubmissionService submissions, Clock clock) { this.tasks=Objects.requireNonNull(tasks);this.submissions=Objects.requireNonNull(submissions);this.clock=Objects.requireNonNull(clock); }
    public String assignReview(String submissionId,String assigneeId,Set<KpiRole> roles){KpiAuthorization.require(roles,KpiPermission.TASK_ASSIGN);if(assigneeId==null||assigneeId.isBlank())throw new IllegalArgumentException("assignee is required");if(submissions.stateOf(submissionId)!=SubmissionState.SUBMITTED)throw new IllegalStateException("only submitted assessments can be assigned");return tasks.create(submissionId,ApprovalTaskRepository.TaskType.REVIEW,assigneeId,clock.instant());}
    public void complete(String taskId,boolean approved,String comment,String actorId,Set<KpiRole> roles){var task=tasks.task(taskId);if(!task.assigneeId().equals(actorId)||task.state()!=ApprovalTaskRepository.TaskState.PENDING)throw new SecurityException("task is not actionable by actor");var target=switch(task.type()){case REVIEW->approved?SubmissionState.CONFIRMED:SubmissionState.RETURNED;case CONFIRM->approved?SubmissionState.CONFIRMED:SubmissionState.RETURNED;case ARCHIVE->SubmissionState.ARCHIVED;};submissions.validateTransition(task.submissionId(),target,roles);tasks.completeAndTransition(taskId,task.submissionId(),target,actorId,comment,clock.instant());}
    public List<ApprovalTaskRepository.Task> pending(String actorId,Set<KpiRole> roles){KpiAuthorization.require(roles,KpiPermission.SUBMISSION_REVIEW);return tasks.pendingFor(actorId);}
}
