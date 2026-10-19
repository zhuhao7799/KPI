package cn.edu.kpi.application.workflow;

import java.time.Instant;
import java.util.List;
import cn.edu.kpi.domain.submission.SubmissionState;

public interface ApprovalTaskRepository {
    String create(String submissionId, TaskType type, String assigneeId, Instant createdAt);
    Task task(String taskId);
    void completeAndTransition(String taskId, String submissionId, SubmissionState target, String actorId, String comment, Instant completedAt);
    List<Task> pendingFor(String assigneeId);
    enum TaskType { REVIEW, CONFIRM, ARCHIVE }
    enum TaskState { PENDING, COMPLETED, CANCELLED }
    record Task(String id, String submissionId, TaskType type, String assigneeId, TaskState state) { }
}
