#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$script_dir"

build_dir="${TMPDIR:-/tmp}/kpi-domain-classes"
rm -rf "$build_dir"
mkdir -p "$build_dir"

jdk_home="${KPI_JDK_HOME:-}"
if [ -z "$jdk_home" ] && command -v javac >/dev/null 2>&1; then
  javac_bin="$(command -v javac)"
  java_bin="$(command -v java)"
elif [ -n "$jdk_home" ] && [ -x "$jdk_home/bin/javac" ]; then
  javac_bin="$jdk_home/bin/javac"
  java_bin="$jdk_home/bin/java"
elif [ -x /workspace/scratch/toolchains/jdk-21.0.9+10/bin/javac ]; then
  javac_bin=/workspace/scratch/toolchains/jdk-21.0.9+10/bin/javac
  java_bin=/workspace/scratch/toolchains/jdk-21.0.9+10/bin/java
else
  echo "JDK 21 is required. Set KPI_JDK_HOME to a JDK installation." >&2
  exit 1
fi

"$javac_bin" --release 21 -d "$build_dir" $(find src/main/java -name '*.java' | sort)
"$java_bin" -cp "$build_dir" cn.edu.kpi.domain.example.CoreExample
"$java_bin" -cp "$build_dir" cn.edu.kpi.domain.example.CoreInvariantTest
