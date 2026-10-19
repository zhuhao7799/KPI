package cn.edu.kpi.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import cn.edu.kpi.domain.model.AssessmentCycle;
import cn.edu.kpi.domain.model.IndicatorVersion;
import cn.edu.kpi.domain.model.OrganizationUnit;
import cn.edu.kpi.domain.score.ScoreCalculator;
import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dependency-free development vertical slice; production will replace this with the platform API. */
public final class KpiDemoServer {
    private static final AssessmentCycle CYCLE = new AssessmentCycle("2026", "2026 年度", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), AssessmentCycle.Status.LOCKED);
    private static final OrganizationUnit UNIT = new OrganizationUnit("unit-demo-001", "示例学院", "COLLEGE", null);
    private static final IndicatorVersion VERSION = new IndicatorVersion("2026-v1", List.of(
        new IndicatorVersion.Indicator("goal", "年度目标完成", new BigDecimal("60"), new BigDecimal("100")),
        new IndicatorVersion.Indicator("service", "服务保障", new BigDecimal("40"), new BigDecimal("100"))));
    private static final SubmissionStore STORE = new SubmissionStore(Path.of(System.getProperty("kpi.data.dir", "/tmp/kpi-demo-data")));
    private static final List<OrganizationImport.Unit> ORGANIZATIONS = OrganizationImport.load();

    public static void main(String[] args) throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 18080), 0);
        server.createContext("/api/health", exchange -> json(exchange, 200, "{\"status\":\"UP\"}"));
        server.createContext("/api/score-preview", KpiDemoServer::preview);
        server.createContext("/api/organization-units", KpiDemoServer::organizationUnits);
        server.createContext("/api/submissions", KpiDemoServer::submissions);
        server.createContext("/api/audit-events", KpiDemoServer::auditEvents);
        server.createContext("/kpi/cycles", KpiDemoServer::cycles);
        server.createContext("/kpi/me", KpiDemoServer::currentActor);
        server.createContext("/kpi/units", KpiDemoServer::formalUnits);
        server.createContext("/kpi/indicator-versions/2026-v1", KpiDemoServer::indicatorVersion);
        server.createContext("/kpi/submissions/preview", KpiDemoServer::formalPreview);
        server.createContext("/kpi/submissions", KpiDemoServer::formalSubmission);
        server.createContext("/kpi/evidence", KpiDemoServer::evidence);
        server.createContext("/kpi/tasks", KpiDemoServer::tasks);
        server.createContext("/kpi/results", KpiDemoServer::results);
        server.createContext("/", KpiDemoServer::index);
        server.start();
        System.out.println("KPI demo server listening on 127.0.0.1:18080");
    }

    /** Temporary dependency-free adapter for the same contract later hosted by RuoYi. */
    private static void cycles(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        json(exchange, 200, "[{\"code\":\"%s\",\"name\":\"%s\",\"startsOn\":\"%s\",\"endsOn\":\"%s\",\"state\":\"%s\"}]".formatted(CYCLE.code(), CYCLE.name(), CYCLE.startsOn(), CYCLE.endsOn(), CYCLE.status()));
    }

    private static void currentActor(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        json(exchange, 200, "{\"id\":\"demo-reviewer\",\"displayName\":\"演示审核员\",\"roles\":[\"SCHOOL_ADMIN\",\"ASSESSMENT_OFFICE\",\"UNIT_EDITOR\",\"REVIEWER\"],\"scope\":\"ALL\"}");
    }

    private static void formalPreview(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var values = Pattern.compile("\\\"rawScore\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(body);
            if (!values.find()) throw new IllegalArgumentException("goal score is required");
            var goal = new BigDecimal(values.group(1));
            if (!values.find()) throw new IllegalArgumentException("service score is required");
            var service = new BigDecimal(values.group(1));
            var result = calculate(goal, service);
            json(exchange, 200, "{\"totalScore\":%s,\"details\":[{\"indicatorCode\":\"goal\",\"rawScore\":%s,\"weightedScore\":%s},{\"indicatorCode\":\"service\",\"rawScore\":%s,\"weightedScore\":%s}]}".formatted(result.totalScore(), goal, result.details().get(0).weightedScore(), service, result.details().get(1).weightedScore()));
        } catch (IllegalArgumentException error) { json(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
    }

    private static void formalSubmission(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("POST".equals(exchange.getRequestMethod()) && path.matches("/kpi/submissions/[^/]+/evidence")) {
            String submissionId = path.substring("/kpi/submissions/".length(), path.length() - "/evidence".length());
            byte[] body = exchange.getRequestBody().readAllBytes();
            if (body.length == 0) { json(exchange, 400, "{\"error\":\"file is required\"}"); return; }
            String id = java.util.UUID.randomUUID().toString();
            json(exchange, 201, "{\"id\":\"" + id + "\",\"filename\":\"上传材料\",\"byteSize\":" + body.length + ",\"sha256\":\"" + "0".repeat(64) + "\",\"submissionId\":\"" + escape(submissionId) + "\"}");
            return;
        }
        if ("POST".equals(exchange.getRequestMethod()) && path.matches("/kpi/submissions/[^/]+/transitions")) {
            String submissionId = path.substring("/kpi/submissions/".length(), path.length() - "/transitions".length());
            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String target = jsonString(body, "target");
                if ("SUBMITTED".equals(target)) { STORE.resubmit(submissionId); json(exchange, 200, "{\"id\":\"" + escape(submissionId) + "\",\"state\":\"SUBMITTED\"}"); }
                else if ("ARCHIVED".equals(target)) { STORE.archive(submissionId); json(exchange, 200, "{\"id\":\"" + escape(submissionId) + "\",\"state\":\"ARCHIVED\"}"); }
                else throw new IllegalArgumentException("unsupported demo transition: " + target);
            } catch (IllegalArgumentException error) { json(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var values = Pattern.compile("\\\"rawScore\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(body);
            if (!values.find()) throw new IllegalArgumentException("goal score is required");
            var goal = new BigDecimal(values.group(1));
            if (!values.find()) throw new IllegalArgumentException("service score is required");
            var service = new BigDecimal(values.group(1));
            var result = calculate(goal, service);
            String id = STORE.save("local-unit-editor", result);
            String taskId = STORE.createReviewTask(id);
            json(exchange, 201, "{\"id\":\"" + id + "\",\"state\":\"SUBMITTED\",\"reviewTaskId\":\""+taskId+"\",\"totalScore\":" + result.totalScore() + "}");
        } catch (IllegalArgumentException | IllegalStateException error) { json(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
    }

    private static void evidence(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("POST".equals(exchange.getRequestMethod()) && path.matches("/kpi/submissions/[^/]+/evidence")) {
            String submissionId = path.substring("/kpi/submissions/".length(), path.length() - "/evidence".length());
            byte[] body = exchange.getRequestBody().readAllBytes();
            if (body.length == 0) { json(exchange, 400, "{\"error\":\"file is required\"}"); return; }
            String id = java.util.UUID.randomUUID().toString();
            json(exchange, 201, "{\"id\":\"" + id + "\",\"filename\":\"上传材料\",\"byteSize\":" + body.length + ",\"sha256\":\"" + "0".repeat(64) + "\",\"submissionId\":\"" + escape(submissionId) + "\"}");
            return;
        }
        if ("POST".equals(exchange.getRequestMethod()) && path.matches("/kpi/evidence/[^/]+/attachments")) {
            String evidenceId = path.substring("/kpi/evidence/".length(), path.length() - "/attachments".length());
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                String submissionId = jsonString(body, "submissionId");
                String indicatorCode = jsonString(body, "indicatorCode");
                STORE.attachEvidence(evidenceId, submissionId, indicatorCode);
                json(exchange, 201, "{\"evidenceId\":\"" + escape(evidenceId) + "\",\"submissionId\":\"" + escape(submissionId) + "\",\"indicatorCode\":\"" + escape(indicatorCode) + "\"}");
            } catch (IllegalArgumentException | IllegalStateException error) { json(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod()) || !"/kpi/evidence".equals(path)) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (!body.contains("storageKey") || !body.contains("sha256")) { json(exchange, 400, "{\"error\":\"private evidence metadata is required\"}"); return; }
        json(exchange, 201, "{\"id\":\"" + STORE.registerEvidence(body) + "\",\"private\":true}");
    }

    private static void tasks(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("POST".equals(exchange.getRequestMethod()) && "/kpi/tasks".equals(path)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                String submissionId = jsonString(body, "submissionId");
                String assigneeId = jsonString(body, "assigneeId");
                STORE.assignTask(submissionId, assigneeId);
                json(exchange, 200, "{\"submissionId\":\"" + escape(submissionId) + "\",\"assigneeId\":\"" + escape(assigneeId) + "\",\"state\":\"PENDING\"}");
            } catch (IllegalArgumentException error) { json(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
            return;
        }
        if ("GET".equals(exchange.getRequestMethod()) && "/kpi/tasks".equals(path)) { json(exchange, 200, "[" + String.join(",", STORE.listTasks()) + "]"); return; }
        if ("POST".equals(exchange.getRequestMethod()) && path.matches("/kpi/tasks/[^/]+/complete")) {
            String taskId = path.substring("/kpi/tasks/".length(), path.length() - "/complete".length());
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                boolean approved = jsonBoolean(body, "approved");
                String state = STORE.completeReviewTask(taskId, approved, optionalJsonString(body, "comment"));
                json(exchange, 200, "{\"id\":\"" + escape(taskId) + "\",\"state\":\"COMPLETED\",\"submissionState\":\"" + state + "\"}");
            } catch (IllegalArgumentException | IllegalStateException error) { json(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
            return;
        }
        json(exchange, 405, "{\"error\":\"method not allowed\"}");
    }

    private static void results(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        String cycleCode = query(exchange.getRequestURI().getRawQuery()).getOrDefault("cycleCode", CYCLE.code());
        var rows = STORE.listResults(cycleCode);
        var result = new java.util.ArrayList<String>();
        int rank = 0; BigDecimal lastScore = null;
        for (int index = 0; index < rows.size(); index++) { var row = rows.get(index); if (lastScore == null || row.totalScore().compareTo(lastScore) != 0) rank = index + 1; lastScore = row.totalScore(); result.add("{\"rank\":" + rank + ",\"submissionId\":\"" + row.submissionId() + "\",\"unitId\":\"" + row.unitId() + "\",\"unitName\":\"示例学院\",\"unitCategory\":\"COLLEGE\",\"totalScore\":" + row.totalScore() + "}"); }
        json(exchange, 200, "[" + String.join(",", result) + "]");
    }

    private static void formalUnits(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        var items = ORGANIZATIONS.stream().map(unit -> "{\"id\":\"%s\",\"name\":\"%s\",\"category\":\"%s\"}".formatted(escape(unit.id()), escape(unit.name()), escape(unit.category()))).toList();
        json(exchange, 200, "[" + String.join(",", items) + "]");
    }

    private static void indicatorVersion(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        json(exchange, 200, "{\"version\":\"2026-v1\",\"formulaVersion\":\"weighted-score-v1\",\"scoreScale\":4,\"indicators\":[{\"code\":\"goal\",\"name\":\"年度目标完成\",\"weight\":60,\"maximumScore\":100},{\"code\":\"service\",\"name\":\"服务保障\",\"weight\":40,\"maximumScore\":100}]}");
    }

    private static void preview(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        try {
            var query = query(exchange.getRequestURI().getRawQuery());
            var goal = decimal(query, "goal");
            var service = decimal(query, "service");
            var result = calculate(goal, service);
            var first = result.details().getFirst();
            var second = result.details().get(1);
            json(exchange, 200, "{\"cycle\":\"%s\",\"unit\":\"%s\",\"indicatorVersion\":\"%s\",\"totalScore\":\"%s\",\"details\":[{\"code\":\"%s\",\"name\":\"年度目标完成\",\"weightedScore\":\"%s\"},{\"code\":\"%s\",\"name\":\"服务保障\",\"weightedScore\":\"%s\"}]}".formatted(result.cycleCode(), UNIT.name(), result.indicatorVersion(), result.totalScore(), first.indicatorCode(), first.weightedScore(), second.indicatorCode(), second.weightedScore()));
        } catch (IllegalArgumentException | IllegalStateException error) {
            json(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}");
        }
    }

    private static void organizationUnits(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        var units = ORGANIZATIONS.stream().map(unit -> "{\"id\":\"%s\",\"name\":\"%s\",\"category\":\"%s\",\"workPlanStatus\":\"%s\"}".formatted(escape(unit.id()), escape(unit.name()), escape(unit.category()), escape(unit.workPlanStatus()))).toList();
        json(exchange, 200, "{\"total\":" + units.size() + ",\"items\":[" + String.join(",", units) + "]}");
    }

    private static void submissions(HttpExchange exchange) throws IOException {
        var query = query(exchange.getRequestURI().getRawQuery());
        if ("GET".equals(exchange.getRequestMethod())) {
            try { KpiAuthorization.require(roles(query), KpiPermission.RESULT_VIEW); json(exchange, 200, "[" + String.join(",", STORE.listSubmissions()) + "]"); }
            catch (SecurityException error) { json(exchange, 403, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        try {
            KpiAuthorization.require(roles(query), KpiPermission.SUBMISSION_CREATE);
            var result = calculate(decimal(query, "goal"), decimal(query, "service"));
            String id = STORE.save(query.getOrDefault("actorId", "demo-reviewer"), result);
            json(exchange, 201, "{\"id\":\"" + id + "\",\"state\":\"SUBMITTED\",\"totalScore\":\"" + result.totalScore() + "\"}");
        } catch (SecurityException error) { json(exchange, 403, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
        catch (IllegalArgumentException | IllegalStateException error) { json(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
    }

    private static void auditEvents(HttpExchange exchange) throws IOException {
        try {
            KpiAuthorization.require(roles(query(exchange.getRequestURI().getRawQuery())), KpiPermission.SUBMISSION_REVIEW);
            json(exchange, 200, "[" + String.join(",", STORE.listAuditEvents()) + "]");
        } catch (SecurityException error) { json(exchange, 403, "{\"error\":\"" + escape(error.getMessage()) + "\"}"); }
    }

    private static ScoreCalculator.ScoreResult calculate(BigDecimal goal, BigDecimal service) {
        return new ScoreCalculator().calculate(CYCLE, UNIT, VERSION, List.of(new ScoreCalculator.ScoreInput("goal", goal, List.of()), new ScoreCalculator.ScoreInput("service", service, List.of())), "preview-user", Instant.EPOCH);
    }

    private static BigDecimal decimal(Map<String, String> query, String field) {
        var value = query.get(field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        try { return new BigDecimal(value); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(field + " must be a decimal number"); }
    }

    private static Map<String, String> query(String raw) {
        var result = new LinkedHashMap<String, String>();
        if (raw == null || raw.isBlank()) return result;
        for (String pair : raw.split("&")) {
            var parts = pair.split("=", 2);
            result.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return result;
    }

    private static Set<KpiRole> roles(Map<String, String> query) {
        String value = query.get("role");
        if (value == null || value.isBlank()) return Set.of();
        var result = java.util.EnumSet.noneOf(KpiRole.class);
        for (String role : value.split(",")) result.add(KpiRole.valueOf(role));
        return result;
    }

    private static String jsonString(String body, String field) { String value = optionalJsonString(body, field); if (value.isBlank()) throw new IllegalArgumentException(field + " is required"); return value; }
    private static String optionalJsonString(String body, String field) { var matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(body); return matcher.find() ? matcher.group(1) : ""; }
    private static boolean jsonBoolean(String body, String field) { var matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(true|false)").matcher(body); if (!matcher.find()) throw new IllegalArgumentException(field + " is required"); return Boolean.parseBoolean(matcher.group(1)); }

    private static void index(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { json(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        var page = new String(KpiDemoServer.class.getResourceAsStream("/web/index.html").readAllBytes(), StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, page.getBytes(StandardCharsets.UTF_8).length);
        exchange.getResponseBody().write(page.getBytes(StandardCharsets.UTF_8));
        exchange.close();
    }

    private static void json(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String escape(String text) { return text.replace("\\", "\\\\").replace("\"", "\\\""); }
}
