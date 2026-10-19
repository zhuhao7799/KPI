#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
frontend_dir="$repo_dir/frontend"
api_log="${TMPDIR:-/tmp}/kpi-module-api.log"
frontend_log="${TMPDIR:-/tmp}/kpi-module-frontend.log"
api_data="${TMPDIR:-/tmp}/kpi-module-preview-data"

KPI_DATA_DIR="$api_data" \
  "$repo_dir/backend/kpi-app/run-demo-server.sh" >"$api_log" 2>&1 &
api_pid=$!

cleanup() {
  kill "$api_pid" 2>/dev/null || true
  [[ -n "${frontend_pid:-}" ]] && kill "$frontend_pid" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

for _ in $(seq 1 30); do
  curl -fsS http://127.0.0.1:18080/api/health >/dev/null 2>&1 && break
  sleep 0.2
done

cd "$frontend_dir"
VITE_APP_API_TARGET=http://127.0.0.1:18080 \
VITE_APP_PORT="${VITE_APP_PORT:-5173}" \
VITE_KPI_DEMO_PREVIEW=true \
  COREPACK_HOME="${COREPACK_HOME:-/workspace/scratch/kpi-platform-poc/corepack}" \
  COREPACK_ENABLE_AUTO_PIN=0 \
  corepack pnpm@10.34.5 dev --host 0.0.0.0 >"$frontend_log" 2>&1 &
frontend_pid=$!

echo "KPI module preview: http://127.0.0.1:${VITE_APP_PORT:-5173}/index"
echo "API health: http://127.0.0.1:18080/api/health"
echo "Logs: $api_log $frontend_log"
wait "$frontend_pid"
