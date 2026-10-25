<p align="center"><img src="mod/src/main/resources/assets/gtnhmcp/logo.png" alt="GTNH MCP" width="512"></p>

A GT New Horizons client mod that runs a read-only [MCP](https://modelcontextprotocol.io) server, so AI clients can read your live NEI recipes, inventory and BetterQuesting progress.

## Install

Needs GTNH on Java 17+. Download the jar from [Releases](https://github.com/sourcelocation/minecraft-gtnh-mcp/releases) into `mods/`, start the game and type `/mcp setup`: it copies the Claude Code command. Other clients connect to `http://127.0.0.1:8765/mcp`; stdio-only ones through `npx mcp-remote http://127.0.0.1:8765/mcp`.

## In game

- `/mcp`: status, plus `setup`, `on`, `off` and `debug`.
- Tool console (`/mcp debug`, or the button in Config): fill in any tool's arguments, run it, and see exactly what the model receives, with its size.
- Config (Mods, or Esc → Mod Options → GTNH MCP): on/off, port, and the tool console.

## Tools

| Tool | Returns |
|---|---|
| `find_items` | Items matching an NEI search, with ids |
| `find_recipes` | Recipes making an item, or using it (`uses`); recycling left out, rare and duplicate sources only counted |
| `get_recipe` | One recipe by id |
| `plan_chain` | Recipe chain from targets to raw materials |
| `get_player` | Position and inventory |
| `get_slot` | One slot's tooltip, ore names, NBT |
| `get_blocks` | Blocks around you grouped by name; `expand` lists chosen ones with position, inventory and fluids |
| `find_blocks` | Every matching block in a larger area, with position, inventory and fluids |
| `label_region`, `list_regions`, `delete_region` | Named areas, e.g. production lines, kept per world |
| `find_veins` | Prospected ore veins, nearest first (with VisualProspecting) |
| `find_quests`, `get_quest` | Quests by what they say or ask for, with progress (with BetterQuesting) |

## Develop

In `mod/`: `./gradlew build`, `./gradlew runClient17`, or `./gradlew install -PgtnhDir=<instance>/.minecraft`. Releases come from [release-please](https://github.com/googleapis/release-please): use [Conventional Commits](https://www.conventionalcommits.org). See [AGENTS.md](AGENTS.md).
