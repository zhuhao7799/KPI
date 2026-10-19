#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
log_file="${TMPDIR:-/tmp}/kpi-demo-server.log"
data_dir="${TMPDIR:-/tmp}/kpi-demo-smoke-data"
rm -rf "$data_dir"
KPI_DATA_DIR="$data_dir" KPI_ORGANIZATION_CSV=/workspace/research/2026年单位导入底稿.csv "$script_dir/run-demo-server.sh" >"$log_file" 2>&1 &
server_pid=$!
trap 'kill "$server_pid" 2>/dev/null || true' EXIT

for _ in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:18080/api/health >/tmp/kpi-health.json 2>/dev/null; then break; fi
  sleep 0.2
done
grep -q '"status":"UP"' /tmp/kpi-health.json
curl -fsS http://127.0.0.1:18080/api/organization-units >/tmp/kpi-units.json
grep -q '"total":69' /tmp/kpi-units.json
grep -q '党政办公室' /tmp/kpi-units.json
curl -fsS http://127.0.0.1:18080/kpi/cycles >/tmp/kpi-cycles.json
grep -q '"state":"LOCKED"' /tmp/kpi-cycles.json
curl -fsS http://127.0.0.1:18080/kpi/me >/tmp/kpi-me.json
grep -q '"displayName":"演示审核员"' /tmp/kpi-me.json
curl -fsS http://127.0.0.1:18080/kpi/units >/tmp/kpi-formal-units.json
grep -q '"id":"unit-1"' /tmp/kpi-formal-units.json
curl -fsS http://127.0.0.1:18080/kpi/indicator-versions/2026-v1 >/tmp/kpi-indicator-version.json
grep -q '"weight":60' /tmp/kpi-indicator-version.json
curl -fsS http://127.0.0.1:18080/ >/tmp/kpi-index.html
grep -q '组织绩效评分预览' /tmp/kpi-index.html
curl -fsS 'http://127.0.0.1:18080/api/score-preview?goal=92&service=88' >/tmp/kpi-preview.json
grep -q '"totalScore":"90.4000"' /tmp/kpi-preview.json
curl -fsS -X POST -H 'Content-Type: application/json' --data '{"scores":[{"indicatorCode":"goal","rawScore":92},{"indicatorCode":"service","rawScore":88}]}' http://127.0.0.1:18080/kpi/submissions/preview >/tmp/kpi-formal-preview.json
grep -q '"totalScore":90.4000' /tmp/kpi-formal-preview.json
curl -fsS -X POST -H 'Content-Type: application/json' --data '{"scores":[{"indicatorCode":"goal","rawScore":92},{"indicatorCode":"service","rawScore":88}]}' http://127.0.0.1:18080/kpi/submissions >/tmp/kpi-formal-submission.json
grep -q '"state":"SUBMITTED"' /tmp/kpi-formal-submission.json
formal_submission_id="$(sed -n 's/.*"id":"\([^"]*\)".*/\1/p' /tmp/kpi-formal-submission.json)"
formal_task_id="$(sed -n 's/.*"reviewTaskId":"\([^"]*\)".*/\1/p' /tmp/kpi-formal-submission.json)"
test -n "$formal_submission_id"
test -n "$formal_task_id"
curl -fsS -X POST -H 'Content-Type: application/json' --data '{"target":"SUBMITTED"}' "http://127.0.0.1:18080/kpi/submissions/$formal_submission_id/transitions" >/tmp/kpi-formal-transition.json
grep -q '"state":"SUBMITTED"' /tmp/kpi-formal-transition.json
curl -fsS -X POST -H 'Content-Type: application/json' --data '{"storageKey":"private/2026/unit-demo-001/goal.pdf","filename":"goal.pdf","mediaType":"application/pdf","byteSize":128,"sha256":"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"}' http://127.0.0.1:18080/kpi/evidence >/tmp/kpi-evidence.json
grep -q '"private":true' /tmp/kpi-evidence.json
evidence_id="$(sed -n 's/.*"id":"\([^"]*\)".*/\1/p' /tmp/kpi-evidence.json)"
test -n "$evidence_id"
curl -fsS -X POST -H 'Content-Type: application/json' --data '{"submissionId":"'"$formal_submission_id"'","indicatorCode":"goal"}' "http://127.0.0.1:18080/kpi/evidence/$evidence_id/attachments" >/tmp/kpi-evidence-link.json
grep -q '"indicatorCode":"goal"' /tmp/kpi-evidence-link.json
curl -fsS http://127.0.0.1:18080/kpi/tasks >/tmp/kpi-tasks.json
grep -q '"state":"PENDING"' /tmp/kpi-tasks.json
curl -fsS -X POST -H 'Content-Type: application/json' --data '{"approved":true,"comment":"材料齐全，审核通过"}' "http://127.0.0.1:18080/kpi/tasks/$formal_task_id/complete" >/tmp/kpi-task-complete.json
grep -q '"submissionState":"CONFIRMED"' /tmp/kpi-task-complete.json
curl -fsS 'http://127.0.0.1:18080/kpi/results?cycleCode=2026' >/tmp/kpi-results.json
grep -q '"rank":1' /tmp/kpi-results.json
grep -q '"totalScore":90.4000' /tmp/kpi-results.json
invalid_status="$(curl -sS -o /tmp/kpi-invalid.json -w '%{http_code}' 'http://127.0.0.1:18080/api/score-preview?goal=101&service=88')"
if [ "$invalid_status" != 400 ]; then
  echo "out-of-range score returned HTTP $invalid_status" >&2
  exit 1
fi
grep -q 'outside range' /tmp/kpi-invalid.json
curl -fsS -X POST 'http://127.0.0.1:18080/api/submissions?goal=92&service=88&actorId=smoke-reviewer&role=UNIT_EDITOR' >/tmp/kpi-submission.json
grep -q '"state":"SUBMITTED"' /tmp/kpi-submission.json
curl -fsS 'http://127.0.0.1:18080/api/submissions?role=RESULT_VIEWER' >/tmp/kpi-submissions.json
grep -q '"formulaVersion":"weighted-score-v1"' /tmp/kpi-submissions.json
curl -fsS 'http://127.0.0.1:18080/api/audit-events?role=REVIEWER' >/tmp/kpi-audit.json
grep -q '"eventType":"ASSESSMENT_SUBMITTED"' /tmp/kpi-audit.json
unauthorized_status="$(curl -sS -o /tmp/kpi-unauthorized.json -w '%{http_code}' -X POST 'http://127.0.0.1:18080/api/submissions?goal=92&service=88&role=RESULT_VIEWER')"
if [ "$unauthorized_status" != 403 ]; then
  echo "unauthorized submission returned HTTP $unauthorized_status" >&2
  exit 1
fi
kill "$server_pid"
wait "$server_pid" 2>/dev/null || true
KPI_DATA_DIR="$data_dir" KPI_ORGANIZATION_CSV=/workspace/research/2026年单位导入底稿.csv "$script_dir/run-demo-server.sh" >"$log_file" 2>&1 &
server_pid=$!
for _ in $(seq 1 30); do
  if curl -fsS 'http://127.0.0.1:18080/api/submissions?role=RESULT_VIEWER' >/tmp/kpi-submissions-restarted.json 2>/dev/null; then break; fi
  sleep 0.2
done
grep -q '"state":"SUBMITTED"' /tmp/kpi-submissions-restarted.json
echo "api-smoke=passed"
