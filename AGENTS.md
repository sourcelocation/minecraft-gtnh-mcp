# GTNH MCP

A fully client-side MCP that provides information to LLMs about a minecraft modpack gregtech new horizons.

GTNH MCP has two components:

- `mod/`: Forge 1.7.10 client mod that reads GTNH Minecraft data
- `mcp/`: Go stdio MCP server that calls the mod over `127.0.0.1:8765`.

Keep the bridge read-only.

scipts/ contain compilation, installation, verification tools.