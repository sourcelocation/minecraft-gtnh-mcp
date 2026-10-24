# Agent notes

Forge 1.7.10 client mod in `mod/` (Java 17, GTNHGradle). It serves stateless Streamable HTTP MCP on `127.0.0.1:8765/mcp` using the official Java SDK and Jetty, both shaded.

## Rules

- Read-only, loopback-only. Never add tools that change game state.
- Every tool call runs on the client thread (`ClientThread`); never read game state elsewhere.
- Output must stay token-lean: data fields only, nulls and empties omitted, short descriptions.
- Prefer the mods' APIs (NEI `StackInfo`, `SearchField`, `RecipeHandlerRef`; BetterQuesting `QuestingAPI`, `QuestTranslation`; GT `GTValues`) over hand-written logic.

## Extending (open/closed)

- New data source: implement `ToolModule`, write tools as `AbstractTool<In, Out>` with record I/O (schemas are generated from the records), and list the module in `META-INF/services/dev.gtnhmcp.core.ToolModule`. Gate optional mods with `available()` and keep their classes out of the module class itself.
- New recipe facts: implement `RecipeDetail` and list it in `META-INF/services/dev.gtnhmcp.recipe.RecipeDetail`.
- Planning policy: implement `PlanPolicy`.

Do not edit existing modules to add a source.

## Verify

`./gradlew build`, then `./gradlew runClient17`, join a world, and call the tools over HTTP.
