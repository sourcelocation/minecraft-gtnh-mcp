package main

import (
	"context"
	"log"

	"github.com/modelcontextprotocol/go-sdk/mcp"
	"github.com/sourcelocation/gtnh-mcp/mcp/internal/bridge"
	"github.com/sourcelocation/gtnh-mcp/mcp/internal/model"
)

func main() {
	client := bridge.New()
	server := newServer(client)
	if err := server.Run(context.Background(), &mcp.StdioTransport{}); err != nil {
		log.Printf("GTNH MCP stopped: %v", err)
	}
}

type bridgeClient interface {
	CallRaw(context.Context, string, interface{}) ([]byte, error)
}

func newServer(client bridgeClient) *mcp.Server {
	server := mcp.NewServer(&mcp.Implementation{Name: "GTNH MCP", Version: "1.0.0"}, nil)

	addBridgeTool(server, client, "status", "Minecraft connection and available capabilities", "status", model.StatusRequest{})
	addBridgeTool(server, client, "player", "Current player position and compact inventory summaries", "player", model.PlayerRequest{})
	addBridgeTool(server, client, "player_slot", "Inspect the exact live item stack in a player inventory slot", "player_slot", model.PlayerSlotRequest{})
	addBridgeTool(server, client, "item_lookup", "Find generic item IDs in the loaded NEI catalog", "item_lookup", model.ItemLookupRequest{})
	addBridgeTool(server, client, "lookup_recipes", "Find recipes that produce a resource", "lookup_recipes", model.RecipeLookupRequest{})
	addBridgeTool(server, client, "lookup_uses", "Find recipes that consume a resource", "lookup_uses", model.RecipeLookupRequest{})
	addBridgeTool(server, client, "recipe_get", "Resolve an opaque NEI recipe identifier", "recipe_get", model.RecipeGetRequest{})
	addBridgeTool(server, client, "recipe_paths", "Build an on-demand recipe chain using live NEI data", "recipe_paths", model.RecipePathsRequest{})
	addBridgeTool(server, client, "quests_query", "Query BetterQuesting progress when available", "quests_query", model.QuestQueryRequest{})

	return server
}

func addBridgeTool[In any](server *mcp.Server, client bridgeClient, name, description, method string, _ In) {
	mcp.AddTool(server, &mcp.Tool{Name: name, Description: description}, func(ctx context.Context, _ *mcp.CallToolRequest, input In) (*mcp.CallToolResult, any, error) {
		output, err := client.CallRaw(ctx, method, input)
		if err != nil {
			return toolError(err), nil, nil
		}
		return &mcp.CallToolResult{Content: []mcp.Content{&mcp.TextContent{Text: string(output)}}}, nil, nil
	})
}

func toolError(err error) *mcp.CallToolResult {
	return &mcp.CallToolResult{
		Content: []mcp.Content{&mcp.TextContent{Text: err.Error()}},
		IsError: true,
	}
}
