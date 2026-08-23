package model

import (
	"encoding/json"
)

type ResourceKey struct {
	Kind   string `json:"kind"`
	ID     string `json:"id"`
	Amount int64  `json:"-"`
	Item   string `json:"item,omitempty"`
}

func (r *ResourceKey) UnmarshalJSON(data []byte) error {
	var wire struct {
		Kind   string `json:"kind"`
		ID     string `json:"id"`
		Amount int64  `json:"amount"`
		Item   string `json:"item"`
	}
	if err := json.Unmarshal(data, &wire); err != nil {
		return err
	}
	r.Kind, r.ID, r.Amount, r.Item = wire.Kind, wire.ID, wire.Amount, wire.Item
	return nil
}

// MarshalJSON keeps planner quantities in the formatted item string while the
// numeric Amount remains available internally for recipe arithmetic.
func (r ResourceKey) MarshalJSON() ([]byte, error) {
	return json.Marshal(struct {
		Kind string `json:"kind,omitempty"`
		ID   string `json:"id"`
		Item string `json:"item,omitempty"`
	}{Kind: r.Kind, ID: r.ID, Item: r.Item})
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
