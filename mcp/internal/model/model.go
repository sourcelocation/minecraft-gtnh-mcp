package model

import (
	"encoding/json"
	"time"
)

// Envelope is attached to every bridge response; Completeness deliberately makes
// client observations distinguishable from authoritative server data.
type Envelope struct {
	Schema       string    `json:"schema"`
	Source       string    `json:"source"`
	CapturedAt   time.Time `json:"captured_at"`
	Completeness string    `json:"completeness"`
	Warnings     []string  `json:"warnings,omitempty"`
}
type ResourceKey struct {
	Kind     string   `json:"kind"`
	ID       string   `json:"id"`
	Registry string   `json:"registry,omitempty"`
	Meta     int      `json:"meta,omitempty"`
	Display  string   `json:"display,omitempty"`
	Amount   int64    `json:"amount,omitempty"`
	NBTHash  string   `json:"nbt_hash,omitempty"`
	OreNames []string `json:"ore_names,omitempty"`
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
