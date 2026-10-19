package cn.edu.kpi.application.assessment;

/** Resolves a submission to its immutable cycle and assessed unit before authorization. */
public interface SubmissionContextReader {
    Context context(String submissionId);

    record Context(String cycleCode, String unitId) { }
}
