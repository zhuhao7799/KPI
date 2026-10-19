package cn.edu.kpi.domain.submission;

/** Executable acceptance check for the submission state machine. */
public final class SubmissionLifecycleTest {
    public static void main(String[] args) {
        if (SubmissionLifecycle.transition(SubmissionState.DRAFT, SubmissionState.SUBMITTED) != SubmissionState.SUBMITTED) {
            throw new AssertionError("draft should submit");
        }
        if (SubmissionLifecycle.transition(SubmissionState.SUBMITTED, SubmissionState.RETURNED) != SubmissionState.RETURNED) {
            throw new AssertionError("submitted should be returnable");
        }
        if (!SubmissionLifecycle.isTerminal(SubmissionState.ARCHIVED)) {
            throw new AssertionError("archived should be terminal");
        }
        try {
            SubmissionLifecycle.transition(SubmissionState.ARCHIVED, SubmissionState.RETURNED);
            throw new AssertionError("archived submission was reopened");
        } catch (IllegalStateException expected) {
            System.out.println("submission-lifecycle=passed");
        }
    }
}
