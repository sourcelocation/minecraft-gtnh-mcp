package planner

import (
	"encoding/json"
	"testing"

	"github.com/sourcelocation/gtnh-mcp/mcp/internal/model"
)

func recipe(id, input, output, machine string) model.Recipe {
	return model.Recipe{
		ID:        json.RawMessage(`"` + id + `"`),
		RecipeMap: machine,
		Inputs:    []model.IngredientGroup{{Alternatives: []model.ResourceKey{{ID: input}}, Consumed: true}},
		Outputs:   []model.IngredientGroup{{Alternatives: []model.ResourceKey{{ID: output}}}},
	}
}

func TestSolveListsRequiredMachines(t *testing.T) {
	recipes := []model.Recipe{recipe("smelt", "ore", "ingot", "furnace"), recipe("bend", "ingot", "plate", "bender")}
	plan := Solve(recipes, Request{Starts: []string{"ore"}, Ends: []string{"plate"}})
	if !plan.Complete || len(plan.Flows) != 2 {
		t.Fatalf("unexpected plan: %#v", plan)
	}
	if len(plan.Machines) != 2 || plan.Machines[0] != "bender" || plan.Machines[1] != "furnace" {
		t.Fatalf("unexpected machines: %#v", plan.Machines)
	}
}

func TestMissingInputIsIncomplete(t *testing.T) {
	plan := Solve([]model.Recipe{recipe("bend", "ingot", "plate", "bender")}, Request{Ends: []string{"plate"}})
	if plan.Complete {
		t.Fatal("missing input must make the chain incomplete")
	}
}
