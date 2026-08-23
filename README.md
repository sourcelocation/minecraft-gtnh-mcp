# GTNH MCP

GTNH MCP is a client-side AI companion for GT New Horizons Minecraft modpack that understands your inventory, quests, recipes, and what to build next.

GTNH MCP pairs a lightweight Minecraft mod with a local MCP server. It is read-only and stays on your machine.

## Install

Currently supports only Linux and macOS.

```sh
GTNH_INSTANCE_DIR=/absolute/path/to/GTNH ./scripts/install.sh
```

This installs the mod jar into the instance and `gtnh-mcp` into `~/.local/bin` by default. Configure your MCP client to run `gtnh-mcp` without arguments. Minecraft can start before or after the MCP server.

## Develop

```sh
./scripts/check.sh
./scripts/build.sh
```

Build artifacts are written to `dist/`.

`AGENTS.md` for agents

## Credits

\- sourcelocation