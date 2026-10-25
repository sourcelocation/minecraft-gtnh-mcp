# Agent notes

Forge 1.7.10 client mod in `mod/` (Java 17, GTNHGradle). It serves stateless Streamable HTTP MCP on `127.0.0.1:<port>/mcp` using the official Java SDK and Jetty, both shaded. GTNHLib (config, client command) and ModularUI2 (debug screen) come from the pack.

## Rules

- Loopback-only, and never change the game. Tools may only write this mod's own notes (region labels) and must say so with `Tool.access()`.
- Every tool call goes through `ToolCatalog`, which runs it on the client thread. Never read game state elsewhere.
- Tools return plain records of facts (`Item`, `Stack`, `RecipeView.Ingredient`, ...), never formatted text. How output reads (labels, omitted nulls and empties, schemas) lives only in `dev.gtnhmcp.output`, so format changes touch nothing else.
- Output must stay token-lean: data fields only, items as game-style labels like `24× Torch [minecraft:torch]`, short descriptions. Check sizes with `/mcp debug`.
- Prefer the mods' APIs (NEI `StackInfo`, `SearchField`, `RecipeHandlerRef`; BetterQuesting `QuestingAPI`, `QuestTranslation`; GT `GTValues`; GTNHLib config and commands; ModularUI2) over hand-written logic.
- Generic before specific: reach every mod through vanilla, Forge or NEI interfaces (`getPickBlock`, `IInventory`, NEI tab names), and accept some lost precision over code for one mod we then have to maintain. Write mod-specific code only for data nothing generic carries (GregTech EU/t in `GregTechDetail`) and ask first.
- Keep GUI code in views; state and behaviour go in GUI-free models (`ToolConsole` for `DebugScreen`).
- Conventional Commits; release-please versions and releases from them.

## Extending (open/closed)

- New data source: implement `ToolModule`, write tools as `AbstractTool<In, Out>` with record I/O (schemas and the debug screen's argument template come from the records), and list the module in `META-INF/services/dev.gtnhmcp.core.ToolModule`. Gate optional mods with `available()` and keep their classes out of the module class itself.
- New recipe facts: implement `RecipeDetail` and list it in `META-INF/services/dev.gtnhmcp.recipe.RecipeDetail`.
- A tab to show only as a count: add its NEI handler id to `RecipeSources.COLLAPSED`. Recycling tabs are recognised by name.
- Another id syntax: implement `item.IdFormat` and add it to `NeiItems`' list.
- Planning policy: implement `PlanPolicy`.
- A value that should read as one string: extend `output.Label` (a Jackson serializer that also describes its schema) and add it in `OutputFormat`.
- New `/mcp` action: implement `Subcommand` and add it in `GtnhMcp`.

Do not edit existing modules to add a source.

## Verify

`./gradlew build`, then `./gradlew runClient17` (or `install` into a real instance), join a world, and use `/mcp debug` or call the tools over HTTP.
