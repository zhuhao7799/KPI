package cn.edu.kpi.app;

import cn.edu.kpi.domain.score.ScoreCalculator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Small append-only persistence adapter for the development slice. */
public final class SubmissionStore {
    private final Path submissions;
    private final Path auditEvents;
    private final Path evidence;
    private final Path evidenceLinks;
    private final Path tasks;
    public SubmissionStore(Path dataDir) {
        try { Files.createDirectories(dataDir); } catch (IOException e) { throw new IllegalStateException("cannot create KPI data directory", e); }
        submissions = dataDir.resolve("assessment-submissions.jsonl");
        auditEvents = dataDir.resolve("assessment-audit-events.jsonl");
        evidence = dataDir.resolve("evidence-objects.jsonl");
        evidenceLinks = dataDir.resolve("submission-evidence.jsonl");
        tasks = dataDir.resolve("approval-tasks.jsonl");
    }
    public synchronized String save(String actorId, ScoreCalculator.ScoreResult result) {
        String id = UUID.randomUUID().toString(); String createdAt = Instant.now().toString();
        String submission = "{\"id\":\"%s\",\"state\":\"SUBMITTED\",\"cycleCode\":\"%s\",\"unitId\":\"%s\",\"indicatorVersion\":\"%s\",\"calculationContext\":{\"formulaVersion\":\"weighted-score-v1\",\"scale\":4},\"totalScore\":\"%s\",\"submittedAt\":\"%s\"}%n".formatted(id, result.cycleCode(), result.unitId(), result.indicatorVersion(), result.totalScore(), createdAt);
        String event = "{\"eventType\":\"ASSESSMENT_SUBMITTED\",\"submissionId\":\"%s\",\"actorId\":\"%s\",\"occurredAt\":\"%s\"}%n".formatted(id, escape(actorId), createdAt);
        try { Files.writeString(submissions, submission, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND); Files.writeString(auditEvents, event, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND); return id; }
        catch (IOException e) { throw new IllegalStateException("cannot persist assessment submission", e); }
    }
    public synchronized List<String> listSubmissions() { return read(submissions, "cannot read assessment submissions"); }
    public synchronized List<String> listAuditEvents() { return read(auditEvents, "cannot read assessment audit events"); }
    public synchronized String registerEvidence(String metadata) { String id=UUID.randomUUID().toString(); append(evidence,"{\"id\":\""+id+"\",\"metadata\":"+metadata+",\"private\":true}" + System.lineSeparator()); return id; }
    public synchronized void attachEvidence(String evidenceId, String submissionId, String indicatorCode) {
        if (!containsId(evidence, evidenceId)) throw new IllegalArgumentException("unknown evidence: " + evidenceId);
        if (!containsId(submissions, submissionId)) throw new IllegalArgumentException("unknown submission: " + submissionId);
        append(evidenceLinks, "{\"evidenceId\":\"" + escape(evidenceId) + "\",\"submissionId\":\"" + escape(submissionId) + "\",\"indicatorCode\":\"" + escape(indicatorCode) + "\"}" + System.lineSeparator());
    }
    public synchronized String createReviewTask(String submissionId) { String id=UUID.randomUUID().toString(); append(tasks,"{\"id\":\""+id+"\",\"submissionId\":\""+submissionId+"\",\"type\":\"REVIEW\",\"assigneeId\":\"local-reviewer\",\"state\":\"PENDING\"}" + System.lineSeparator()); return id; }
    public synchronized void resubmit(String submissionId) {
        var current = read(submissions, "cannot read assessment submissions");
        var updated = new java.util.ArrayList<String>(); boolean found = false; boolean returned = false;
        for (String submission : current) {
            if (!hasId(submission, submissionId)) { updated.add(submission); continue; }
            found = true; returned = submission.contains("\"state\":\"RETURNED\"");
            updated.add(returned ? submission.replace("\"state\":\"RETURNED\"", "\"state\":\"SUBMITTED\"") : submission);
        }
        if (!found) throw new IllegalArgumentException("unknown submission: " + submissionId);
        write(submissions, updated);
        if (returned) createReviewTask(submissionId);
    }
    public synchronized void archive(String submissionId) {
        var current = read(submissions, "cannot read assessment submissions");
        var updated = new java.util.ArrayList<String>(); boolean found = false; boolean confirmed = false;
        for (String submission : current) {
            if (!hasId(submission, submissionId)) { updated.add(submission); continue; }
            found = true; confirmed = submission.contains("\"state\":\"CONFIRMED\"");
            updated.add(confirmed ? submission.replace("\"state\":\"CONFIRMED\"", "\"state\":\"ARCHIVED\"") : submission);
        }
        if (!found) throw new IllegalArgumentException("unknown submission: " + submissionId);
        if (!confirmed) throw new IllegalStateException("only confirmed submissions can be archived");
        write(submissions, updated);
    }
    public synchronized List<String> listTasks() { return read(tasks, "cannot read approval tasks"); }
    public synchronized String completeReviewTask(String taskId, boolean approved, String comment) {
        var currentTasks = read(tasks, "cannot read approval tasks");
        String submissionId = null;
        var updatedTasks = new java.util.ArrayList<String>();
        for (String task : currentTasks) {
            if (!hasId(task, taskId)) { updatedTasks.add(task); continue; }
            if (!task.contains("\"state\":\"PENDING\"")) throw new IllegalStateException("task is no longer pending");
            submissionId = field(task, "submissionId");
            updatedTasks.add(task.replace("\"state\":\"PENDING\"", "\"state\":\"COMPLETED\",\"decision\":\"" + (approved ? "APPROVED" : "RETURNED") + "\",\"comment\":\"" + escape(comment) + "\""));
        }
        if (submissionId == null) throw new IllegalArgumentException("unknown task: " + taskId);
        write(tasks, updatedTasks);
        String targetState = approved ? "CONFIRMED" : "RETURNED";
        var updatedSubmissions = new java.util.ArrayList<String>();
        for (String submission : read(submissions, "cannot read assessment submissions")) {
            updatedSubmissions.add(hasId(submission, submissionId) ? submission.replace("\"state\":\"SUBMITTED\"", "\"state\":\"" + targetState + "\"") : submission);
        }
        write(submissions, updatedSubmissions);
        return targetState;
    }
    public synchronized List<Result> listResults(String cycleCode) {
        var rows = new java.util.ArrayList<Result>();
        for (String submission : read(submissions, "cannot read assessment submissions")) {
            if ((submission.contains("\"state\":\"CONFIRMED\"") || submission.contains("\"state\":\"ARCHIVED\"")) && cycleCode.equals(field(submission, "cycleCode"))) {
                rows.add(new Result(field(submission, "id"), field(submission, "unitId"), new java.math.BigDecimal(field(submission, "totalScore"))));
            }
        }
        rows.sort(java.util.Comparator.comparing(Result::totalScore).reversed().thenComparing(Result::submissionId));
        return List.copyOf(rows);
    }
    public record Result(String submissionId, String unitId, java.math.BigDecimal totalScore) { }
    private static void append(Path file,String line){try{Files.writeString(file,line,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(IOException e){throw new IllegalStateException("cannot persist KPI data",e);}}
    private static List<String> read(Path file, String message) { try { return Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : List.of(); } catch (IOException e) { throw new IllegalStateException(message, e); } }
    private static void write(Path file, List<String> lines) { try { Files.writeString(file, String.join(System.lineSeparator(), lines) + (lines.isEmpty() ? "" : System.lineSeparator()), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); } catch (IOException e) { throw new IllegalStateException("cannot persist KPI data", e); } }
    private static boolean containsId(Path file, String id) { return read(file, "cannot read KPI data").stream().anyMatch(line -> hasId(line, id)); }
    private static boolean hasId(String line, String id) { return ("\"id\":\"" + id + "\"").equals("\"id\":\"" + field(line, "id") + "\""); }
    private static String field(String line, String field) { var matcher = java.util.regex.Pattern.compile("\\\"" + java.util.regex.Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(line); if (!matcher.find()) throw new IllegalArgumentException("missing field: " + field); return matcher.group(1); }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
