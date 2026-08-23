#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

(cd "$repo_dir/mod" && VERSION=${VERSION:-0.0.0-dev} ./gradlew check)
(cd "$repo_dir/mcp" && go test ./...)
