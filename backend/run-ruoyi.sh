#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ruoyi_dir="$script_dir/ruoyi-vue-plus"
jdk_home="${KPI_JDK_HOME:-/workspace/scratch/toolchains/jdk-21.0.9+10}"
mvn_bin="${KPI_MAVEN_HOME:-/workspace/scratch/kpi-platform-poc/apache-maven-3.9.11}/bin/mvn"

if [[ ! -x "$mvn_bin" ]]; then echo "Maven 3.9+ not found; set KPI_MAVEN_HOME" >&2; exit 1; fi
if [[ ! -x "$jdk_home/bin/java" ]]; then echo "JDK 21 not found; set KPI_JDK_HOME" >&2; exit 1; fi
bash "$script_dir/apply-ruoyi-integration.sh"
cd "$ruoyi_dir"
JAVA_HOME="$jdk_home" "$mvn_bin" -pl ruoyi-admin -am spring-boot:run \
  -Dspring-boot.run.arguments="--spring.config.additional-location=optional:file:../../deploy/backend-config/"
