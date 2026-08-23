package main

import (
	"context"
	"encoding/json"
	"testing"

	"github.com/modelcontextprotocol/go-sdk/mcp"
	"github.com/sourcelocation/gtnh-mcp/mcp/internal/bridge"
)

type fakeBridge struct {
	response []byte
}

func (f *fakeBridge) CallRaw(context.Context, string, interface{}) ([]byte, error) {
	return f.response, nil
}

func (f *fakeBridge) Call(_ context.Context, _ string, _ interface{}, out interface{}) error {
	return json.Unmarshal(f.response, out)
}

func TestServerPublishesExpectedTools(t *testing.T) {
	ctx := context.Background()
	clientTransport, serverTransport := mcp.NewInMemoryTransports()
	serverSession, err := newServer(bridge.New()).Connect(ctx, serverTransport, nil)
	if err != nil {
		t.Fatal(err)
	}
	defer serverSession.Close()

	client := mcp.NewClient(&mcp.Implementation{Name: "test", Version: "1"}, nil)
	clientSession, err := client.Connect(ctx, clientTransport, nil)
	if err != nil {
		t.Fatal(err)
	}
	defer clientSession.Close()

	result, err := clientSession.ListTools(ctx, nil)
	if err != nil {
		t.Fatal(err)
	}
	if len(result.Tools) != 8 {
		t.Fatalf("expected 8 tools, got %d", len(result.Tools))
	}
	for _, tool := range result.Tools {
		if tool.InputSchema == nil {
			t.Fatalf("tool %q is missing an input schema", tool.Name)
		}
		if tool.Name == "recipe_paths" && tool.OutputSchema == nil {
			t.Fatal("recipe_paths is missing its output schema")
		}
		if tool.Name != "recipe_paths" && tool.OutputSchema != nil {
			t.Fatalf("forwarded tool %q unexpectedly has an output schema", tool.Name)
		}
	}
}

func TestBridgeToolForwardsJavaJSONAsText(t *testing.T) {
	ctx := context.Background()
	payload := []byte(`{"connected":true}`)
	clientTransport, serverTransport := mcp.NewInMemoryTransports()
	serverSession, err := newServer(&fakeBridge{response: payload}).Connect(ctx, serverTransport, nil)
	if err != nil {
		t.Fatal(err)
	}
	defer serverSession.Close()

	client := mcp.NewClient(&mcp.Implementation{Name: "test", Version: "1"}, nil)
	clientSession, err := client.Connect(ctx, clientTransport, nil)
	if err != nil {
		t.Fatal(err)
	}
	defer clientSession.Close()

	result, err := clientSession.CallTool(ctx, &mcp.CallToolParams{Name: "status"})
	if err != nil {
		t.Fatal(err)
	}
	if result.IsError || len(result.Content) != 1 {
		t.Fatalf("unexpected tool result: %#v", result)
	}
	text, ok := result.Content[0].(*mcp.TextContent)
	if !ok || text.Text != string(payload) {
		t.Fatalf("tool did not preserve bridge JSON: %#v", result.Content)
	}
	if result.StructuredContent != nil {
		t.Fatalf("forwarded tool unexpectedly returned structured content: %#v", result.StructuredContent)
	}
}
