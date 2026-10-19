package cn.edu.kpi.infrastructure.jdbc;

import cn.edu.kpi.application.workflow.ApprovalTaskRepository;
import cn.edu.kpi.domain.submission.SubmissionLifecycle;
import cn.edu.kpi.domain.submission.SubmissionState;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Commits a task decision, submission state and audit event in one transaction. */
public final class JdbcApprovalTaskRepository implements ApprovalTaskRepository {
    private final DataSource dataSource;

    public JdbcApprovalTaskRepository(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public String create(String submissionId, TaskType type, String assigneeId, Instant at) {
        String id = UUID.randomUUID().toString();
        var sql = "insert into assessment_approval_task (id,assessment_submission_id,task_type,assignee_id,state,created_at) values (?,?,?,?,'PENDING',?)";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, id); statement.setString(2, submissionId); statement.setString(3, type.name());
            statement.setString(4, assigneeId); statement.setObject(5, at); statement.executeUpdate();
            return id;
        } catch (SQLException error) { throw new IllegalStateException("cannot create approval task", error); }
    }

    @Override public Task task(String id) {
        var sql = "select id,assessment_submission_id,task_type,assignee_id,state from assessment_approval_task where id=?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) throw new IllegalArgumentException("unknown task: " + id);
                return row(rows);
            }
        } catch (SQLException error) { throw new IllegalStateException("cannot read approval task", error); }
    }

    @Override public void completeAndTransition(String taskId, String submissionId, SubmissionState target,
                                                String actorId, String comment, Instant at) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Task lockedTask;
                try (var statement = connection.prepareStatement("select id,assessment_submission_id,task_type,assignee_id,state from assessment_approval_task where id=? for update")) {
                    statement.setString(1, taskId);
                    try (var rows = statement.executeQuery()) {
                        if (!rows.next()) throw new IllegalArgumentException("unknown task: " + taskId);
                        lockedTask = row(rows);
                    }
                }
                if (!lockedTask.submissionId().equals(submissionId) || !lockedTask.assigneeId().equals(actorId)
                    || lockedTask.state() != TaskState.PENDING) throw new SecurityException("task is not actionable by actor");
                SubmissionState current;
                try (var statement = connection.prepareStatement("select state from assessment_submission where id=? for update")) {
                    statement.setString(1, submissionId);
                    try (var rows = statement.executeQuery()) {
                        if (!rows.next()) throw new IllegalArgumentException("unknown submission: " + submissionId);
                        current = SubmissionState.valueOf(rows.getString(1));
                    }
                }
                SubmissionLifecycle.transition(current, target);
                try (var statement = connection.prepareStatement("update assessment_submission set state=? where id=?")) {
                    statement.setString(1, target.name()); statement.setString(2, submissionId);
                    if (statement.executeUpdate() != 1) throw new IllegalStateException("submission changed during task completion");
                }
                try (var statement = connection.prepareStatement("update assessment_approval_task set state='COMPLETED',comment_text=?,completed_at=? where id=? and state='PENDING'")) {
                    statement.setString(1, comment); statement.setObject(2, at); statement.setString(3, taskId);
                    if (statement.executeUpdate() != 1) throw new IllegalStateException("task is no longer pending");
                }
                try (var statement = connection.prepareStatement("insert into assessment_audit_event (id,aggregate_type,aggregate_id,event_type,actor_id,occurred_at,payload_json) values (?,?,?,?,?,?,json_object('taskId',?,'comment',?))")) {
                    statement.setString(1, UUID.randomUUID().toString()); statement.setString(2, "ASSESSMENT_SUBMISSION");
                    statement.setString(3, submissionId); statement.setString(4, "STATE_" + target.name());
                    statement.setString(5, actorId); statement.setObject(6, at); statement.setString(7, taskId); statement.setString(8, comment);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (Exception error) { connection.rollback(); throw error; }
        } catch (SQLException error) { throw new IllegalStateException("cannot complete approval task", error); }
    }

    @Override public List<Task> pendingFor(String assigneeId) {
        var sql = "select id,assessment_submission_id,task_type,assignee_id,state from assessment_approval_task where assignee_id=? and state='PENDING' order by created_at";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, assigneeId);
            try (var rows = statement.executeQuery()) {
                var result = new ArrayList<Task>();
                while (rows.next()) result.add(row(rows));
                return List.copyOf(result);
            }
        } catch (SQLException error) { throw new IllegalStateException("cannot list approval tasks", error); }
    }

    private static Task row(ResultSet rows) throws SQLException {
        return new Task(rows.getString(1), rows.getString(2), TaskType.valueOf(rows.getString(3)),
            rows.getString(4), TaskState.valueOf(rows.getString(5)));
    }
}
