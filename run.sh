#!/usr/bin/env bash
# Launch the HKUST Marketplace with a workspace-local Maven home and repository.
#
# This avoids writes to ~/.m2, which are blocked in some sandboxed/restricted
# environments ("mkdir .../.m2/wrapper: Operation not permitted").
set -euo pipefail

cd "$(dirname "$0")"

export MAVEN_USER_HOME="${MAVEN_USER_HOME:-$PWD/.mvn-home}"
MAVEN_REPO="${MAVEN_LOCAL_REPO:-$PWD/.m2-repository}"

exec ./mvnw -Dmaven.repo.local="$MAVEN_REPO" clean compile javafx:run
