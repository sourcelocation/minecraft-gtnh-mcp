package main

import (
	"context"
	"log"

	"github.com/modelcontextprotocol/go-sdk/mcp"
	"github.com/sourcelocation/gtnh-mcp/mcp/internal/bridge"
	"github.com/sourcelocation/gtnh-mcp/mcp/internal/model"
	"github.com/sourcelocation/gtnh-mcp/mcp/internal/planner"
)

func main() {
	client := bridge.New()
	server := newServer(client)
	if err := server.Run(context.Background(), &mcp.StdioTransport{}); err != nil {
		log.Printf("GTNH MCP stopped: %v", err)
	}
}

type bridgeClient interface {
	Call(context.Context, string, interface{}, interface{}) error
	CallRaw(context.Context, string, interface{}) ([]byte, error)
}

func newServer(client bridgeClient) *mcp.Server {
	server := mcp.NewServer(&mcp.Implementation{Name: "GTNH MCP", Version: "1.0.0"}, nil)

	addBridgeTool(server, client, "status", "Minecraft connection and available capabilities", "status", model.StatusRequest{})
	addBridgeTool(server, client, "player", "Current player position and compact inventory summaries", "player", model.PlayerRequest{})
	addBridgeTool(server, client, "player_slot", "Inspect the exact live item stack in a player inventory slot", "player_slot", model.PlayerSlotRequest{})
	addBridgeTool(server, client, "item_lookup", "Find generic item IDs in the loaded NEI catalog", "item_lookup", model.ItemLookupRequest{})
	addBridgeTool(server, client, "recipe_lookup", "Find recipes that produce or consume a resource", "recipe_lookup", model.RecipeLookupRequest{})
	addBridgeTool(server, client, "recipe_get", "Resolve an opaque NEI recipe identifier", "recipe_get", model.RecipeGetRequest{})
	addBridgeTool(server, client, "quests_query", "Query BetterQuesting progress when available", "quests_query", model.QuestQueryRequest{})

	mcp.AddTool(server, &mcp.Tool{Name: "recipe_paths", Description: "Build a bounded recipe chain and list the machines it requires"}, recipePaths(client))

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

func recipePaths(client bridgeClient) func(context.Context, *mcp.CallToolRequest, planner.Request) (*mcp.CallToolResult, planner.Plan, error) {
	return func(ctx context.Context, _ *mcp.CallToolRequest, request planner.Request) (*mcp.CallToolResult, planner.Plan, error) {
		var graph model.RecipeData
		params := model.GraphRequest{Starts: request.Starts, Ends: request.Ends, MaxDepth: request.MaxSteps, MaxRecipes: 300}
		if err := client.Call(ctx, "recipes.graph", params, &graph); err != nil {
			return toolError(err), planner.Plan{}, nil
		}
		return nil, planner.Solve(graph.Recipes, request), nil
	}
}

func toolError(err error) *mcp.CallToolResult {
	return &mcp.CallToolResult{
		Content: []mcp.Content{&mcp.TextContent{Text: err.Error()}},
		IsError: true,
	}
}
