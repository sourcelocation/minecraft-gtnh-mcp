package model

import (
	"encoding/json"
)

type ResourceKey struct {
	Kind   string `json:"kind"`
	ID     string `json:"id"`
	Amount int64  `json:"amount,omitempty"`
	Text   string `json:"text,omitempty"`
}
type IngredientGroup struct {
	Alternatives []ResourceKey `json:"alternatives"`
	Consumed     bool          `json:"consumed"`
	Catalyst     bool          `json:"catalyst"`
	Chance       float64       `json:"chance,omitempty"`
}
type Recipe struct {
	ID          json.RawMessage   `json:"id"`
	HandlerID   string            `json:"handler_id"`
	HandlerName string            `json:"handler_name,omitempty"`
	Inputs      []IngredientGroup `json:"inputs"`
	Outputs     []IngredientGroup `json:"outputs"`
	Category    string            `json:"category,omitempty"`
	RecipeMap   string            `json:"recipe_map,omitempty"`
}
