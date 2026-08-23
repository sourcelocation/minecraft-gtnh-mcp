#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
: "${GTNH_INSTANCE_DIR:?set GTNH_INSTANCE_DIR to the Minecraft instance directory}"
bin_dir=${GTNH_MCP_BIN_DIR:-"$HOME/.local/bin"}
mods_dir="$GTNH_INSTANCE_DIR/mods"

if [[ ! -d "$GTNH_INSTANCE_DIR" ]]; then
  echo "Minecraft instance does not exist: $GTNH_INSTANCE_DIR" >&2
  exit 1
fi

"$repo_dir/scripts/build.sh"
mkdir -p "$bin_dir" "$mods_dir"
install -m 0755 "$repo_dir/dist/gtnh-mcp" "$bin_dir/gtnh-mcp"

install -m 0644 "$repo_dir/dist/gtnh-mcp-mod.jar" "$mods_dir/gtnh-mcp-mod.jar"

printf 'Installed MCP server to %s and mod to %s\n' "$bin_dir" "$mods_dir"
