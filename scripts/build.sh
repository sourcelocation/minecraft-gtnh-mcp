#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
dist_dir="$repo_dir/dist"

mkdir -p "$dist_dir"
gradle_tasks=(reobfJar)
if [[ -n ${VERSION:-} ]]; then
  gradle_tasks=(clean reobfJar)
fi
(cd "$repo_dir/mod" && VERSION=${VERSION:-0.0.0-dev} ./gradlew "${gradle_tasks[@]}")
(cd "$repo_dir/mcp" && go build -trimpath -o "$dist_dir/gtnh-mcp" ./cmd/gtnh-mcp)

mod_jars=()
while IFS= read -r jar; do
  if [[ -f "${jar%.jar}-dev.jar" ]]; then
    mod_jars+=("$jar")
  fi
done < <(find "$repo_dir/mod/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' | sort)
if [[ ${#mod_jars[@]} -ne 1 ]]; then
  echo "expected one distributable mod jar, found ${#mod_jars[@]}" >&2
  exit 1
fi
cp "${mod_jars[0]}" "$dist_dir/gtnh-mcp-mod.jar"

printf 'Built:\n  %s\n  %s\n' "$dist_dir/gtnh-mcp" "$dist_dir/gtnh-mcp-mod.jar"
