package cn.edu.kpi.domain.submission;

import java.util.Map;
import java.util.Set;

/** Authoritative state-transition rules for an assessment submission. */
public final class SubmissionLifecycle {
    private static final Map<SubmissionState, Set<SubmissionState>> TRANSITIONS = Map.of(
        SubmissionState.DRAFT, Set.of(SubmissionState.SUBMITTED, SubmissionState.VOID),
        SubmissionState.SUBMITTED, Set.of(SubmissionState.RETURNED, SubmissionState.CONFIRMED, SubmissionState.VOID),
        SubmissionState.RETURNED, Set.of(SubmissionState.SUBMITTED, SubmissionState.VOID),
        SubmissionState.CONFIRMED, Set.of(SubmissionState.ARCHIVED, SubmissionState.RETURNED),
        SubmissionState.ARCHIVED, Set.of(),
        SubmissionState.VOID, Set.of());

    private SubmissionLifecycle() { }

    public static SubmissionState transition(SubmissionState current, SubmissionState target) {
        if (current == null || target == null) throw new IllegalArgumentException("submission states are required");
        if (!TRANSITIONS.get(current).contains(target)) {
            throw new IllegalStateException("transition from " + current + " to " + target + " is not allowed");
        }
        return target;
    }

    public static boolean isTerminal(SubmissionState state) {
        return state == SubmissionState.ARCHIVED || state == SubmissionState.VOID;
    }
}
