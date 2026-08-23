#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
dist_dir="$repo_dir/dist"

mkdir -p "$dist_dir"
(cd "$repo_dir/mod" && VERSION=${VERSION:-0.0.0-dev} ./gradlew stageModJar)
(cd "$repo_dir/mcp" && go build -trimpath -o "$dist_dir/gtnh-mcp" ./cmd/gtnh-mcp)

printf 'Built:\n  %s\n  %s\n' "$dist_dir/gtnh-mcp" "$dist_dir/gtnh-mcp-mod.jar"
