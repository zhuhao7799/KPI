#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
repo_dir="$(CDPATH= cd -- "$script_dir/../.." && pwd)"
build_dir="${TMPDIR:-/tmp}/kpi-demo-classes"
rm -rf "$build_dir"
mkdir -p "$build_dir"

jdk_home="${KPI_JDK_HOME:-/workspace/scratch/toolchains/jdk-21.0.9+10}"
if [ ! -x "$jdk_home/bin/javac" ]; then
  echo "JDK 21 is required. Set KPI_JDK_HOME to a JDK installation." >&2
  exit 1
fi

find "$repo_dir/backend/kpi-domain/src/main/java" "$script_dir/src/main/java" -name '*.java' -print | sort > "$build_dir/sources.txt"
"$jdk_home/bin/javac" --release 21 --add-modules jdk.httpserver -d "$build_dir" @"$build_dir/sources.txt"
cp -R "$script_dir/src/main/resources/." "$build_dir/"
data_dir="${KPI_DATA_DIR:-${TMPDIR:-/tmp}/kpi-demo-data}"
exec "$jdk_home/bin/java" --add-modules jdk.httpserver -Dkpi.data.dir="$data_dir" -cp "$build_dir" cn.edu.kpi.app.KpiDemoServer
