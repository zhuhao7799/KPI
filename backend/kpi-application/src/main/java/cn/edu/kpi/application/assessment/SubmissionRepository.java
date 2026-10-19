package cn.edu.kpi.application.assessment;

import cn.edu.kpi.domain.score.ScoreCalculator;
import cn.edu.kpi.domain.submission.SubmissionState;

import java.time.Instant;

/** Write port; the RuoYi/MyBatis adapter will persist it transactionally with an audit event. */
public interface SubmissionRepository {
    String createDraft(ScoreCalculator.ScoreResult score, Instant occurredAt);

    SubmissionState stateOf(String submissionId);

    void changeState(String submissionId, SubmissionState expected, SubmissionState target, String actorId, Instant occurredAt);
}
