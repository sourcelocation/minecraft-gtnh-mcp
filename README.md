# GTNH MCP

A GT New Horizons client mod that runs a read-only [MCP](https://modelcontextprotocol.io) server, so AI clients can read your live NEI recipes, inventory and BetterQuesting progress.

## Install

Needs GTNH on Java 17+.

1. `./gradlew install -PgtnhDir=<instance>/.minecraft`, or copy `mod/build/libs/gtnh-mcp-*.jar` into `mods/`.
2. While the game runs, point your client at `http://127.0.0.1:8765/mcp`:
   - Claude Code: `claude mcp add --transport http gtnh http://127.0.0.1:8765/mcp`
   - stdio-only clients: `npx mcp-remote http://127.0.0.1:8765/mcp`

## Tools

| Tool | Returns |
|---|---|
| `find_items` | Items matching an NEI search, with ids |
| `find_recipes` | Recipes making an item, or using it (`uses`) |
| `get_recipe` | One recipe by id |
| `plan_chain` | Recipe chain from targets to raw materials |
| `get_player` | Position and inventory |
| `get_slot` | One slot's tooltip, ore names, NBT |
| `find_quests`, `get_quest` | Quest progress (with BetterQuesting) |

## Develop

`./gradlew build` builds the jar; `./gradlew runClient17` starts a dev client. See [AGENTS.md](AGENTS.md).
