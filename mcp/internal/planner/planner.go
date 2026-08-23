// Package planner builds a small deterministic recipe chain.
package planner

import (
	"encoding/json"
	"sort"

	"github.com/sourcelocation/gtnh-mcp/mcp/internal/model"
)

type Request struct {
	Starts            []string `json:"starts,omitempty"`
	Ends              []string `json:"ends"`
	ExcludesResources []string `json:"excludes_resources,omitempty"`
	MaxSteps          int      `json:"max_steps,omitempty"`
}

type Flow struct {
	RecipeID json.RawMessage         `json:"recipe_id"`
	Machine  string                  `json:"machine"`
	Runs     float64                 `json:"runs"`
	Inputs   []model.IngredientGroup `json:"inputs"`
	Outputs  []model.IngredientGroup `json:"outputs"`
}

type Plan struct {
	Flows    []Flow             `json:"flows"`
	Balance  map[string]float64 `json:"balance"`
	Machines []string           `json:"machines"`
	Complete bool               `json:"complete"`
	Warnings []string           `json:"warnings,omitempty"`
}

func Solve(recipes []model.Recipe, request Request) Plan {
	if request.MaxSteps < 1 {
		request.MaxSteps = 16
	}
	if request.MaxSteps > 64 {
		request.MaxSteps = 64
	}

	plan := Plan{Balance: map[string]float64{}, Complete: true}
	starts := stringSet(request.Starts)
	excluded := stringSet(request.ExcludesResources)
	active := map[string]bool{}
	machines := map[string]bool{}

	for _, target := range request.Ends {
		plan.Balance[target]--
	}

	var produce func(string)
	produce = func(want string) {
		if plan.Balance[want] >= 0 {
			return
		}
		if starts[want] {
			plan.Balance[want] = 0
			return
		}
		if excluded[want] {
			plan.Complete = false
			plan.Warnings = append(plan.Warnings, "excluded resource required: "+want)
			return
		}
		if len(plan.Flows) >= request.MaxSteps {
			plan.Complete = false
			plan.Warnings = append(plan.Warnings, "recipe chain reached max_steps")
			return
		}
		if active[want] {
			plan.Complete = false
			plan.Warnings = append(plan.Warnings, "recipe cycle at "+want)
			return
		}

		candidates := candidatesFor(recipes, want, excluded)
		if len(candidates) == 0 {
			plan.Complete = false
			plan.Warnings = append(plan.Warnings, "no recipe produces "+want)
			return
		}

		active[want] = true
		defer delete(active, want)
		recipe := candidates[0]
		runs := -plan.Balance[want] / outputAmount(recipe, want)
		if runs < 1 {
			runs = 1
		}
		machine := machineName(recipe)
		machines[machine] = true
		plan.Flows = append(plan.Flows, Flow{RecipeID: recipe.ID, Machine: machine, Runs: runs, Inputs: recipe.Inputs, Outputs: recipe.Outputs})

		for _, group := range recipe.Outputs {
			resource := choose(group)
			plan.Balance[resource.ID] += amount(resource) * runs
		}
		for _, group := range recipe.Inputs {
			if !group.Consumed {
				continue
			}
			resource := choose(group)
			plan.Balance[resource.ID] -= amount(resource) * runs
			produce(resource.ID)
		}
	}

	for _, target := range request.Ends {
		produce(target)
	}
	for resource, balance := range plan.Balance {
		if balance < 0 && !starts[resource] {
			plan.Complete = false
		}
	}
	for machine := range machines {
		plan.Machines = append(plan.Machines, machine)
	}
	sort.Strings(plan.Machines)
	return plan
}

func candidatesFor(recipes []model.Recipe, want string, excluded map[string]bool) []model.Recipe {
	var candidates []model.Recipe
	for _, recipe := range recipes {
		if outputAmount(recipe, want) <= 0 || usesExcludedResource(recipe, excluded) {
			continue
		}
		candidates = append(candidates, recipe)
	}
	sort.Slice(candidates, func(i, j int) bool { return string(candidates[i].ID) < string(candidates[j].ID) })
	return candidates
}

func machineName(recipe model.Recipe) string {
	if recipe.RecipeMap != "" {
		return recipe.RecipeMap
	}
	if recipe.Category != "" {
		return recipe.Category
	}
	if recipe.HandlerName != "" {
		return recipe.HandlerName
	}
	return recipe.HandlerID
}

func stringSet(values []string) map[string]bool {
	set := map[string]bool{}
	for _, value := range values {
		set[value] = true
	}
	return set
}

func amount(resource model.ResourceKey) float64 {
	if resource.Amount > 0 {
		return float64(resource.Amount)
	}
	return 1
}

func choose(group model.IngredientGroup) model.ResourceKey {
	best := group.Alternatives[0]
	for _, alternative := range group.Alternatives[1:] {
		if alternative.ID < best.ID {
			best = alternative
		}
	}
	return best
}

func outputAmount(recipe model.Recipe, id string) float64 {
	amountProduced := 0.0
	for _, group := range recipe.Outputs {
		for _, alternative := range group.Alternatives {
			if alternative.ID == id {
				chance := group.Chance
				if chance == 0 {
					chance = 1
				}
				amountProduced += amount(alternative) * chance
			}
		}
	}
	return amountProduced
}

func usesExcludedResource(recipe model.Recipe, excluded map[string]bool) bool {
	for _, groups := range [][]model.IngredientGroup{recipe.Inputs, recipe.Outputs} {
		for _, group := range groups {
			for _, alternative := range group.Alternatives {
				if excluded[alternative.ID] {
					return true
				}
			}
		}
	}
	return false
}
