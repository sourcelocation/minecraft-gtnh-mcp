package model

import "encoding/json"

type ItemLookupRequest struct {
	Query  string `json:"query"`
	Offset int    `json:"offset,omitempty"`
	Limit  int    `json:"limit,omitempty"`
}
type RecipeLookupRequest struct {
	Direction string           `json:"direction"`
	Resource  ResourceSelector `json:"resource"`
	Offset    int              `json:"offset,omitempty"`
	Limit     int              `json:"limit,omitempty"`
}
type ResourceSelector struct {
	ID    string `json:"id,omitempty"`
	Query string `json:"query,omitempty"`
}
type ExactResourceSelector struct {
	ID string `json:"id,omitempty"`
}
type RecipeData struct {
	Recipes           []Recipe `json:"recipes"`
	NextOffset        int      `json:"next_offset,omitempty"`
	Truncated         bool     `json:"truncated,omitempty"`
	TruncationReasons []string `json:"truncation_reasons,omitempty"`
}
type RecipeGetRequest struct {
	ID json.RawMessage `json:"id"`
}
type GraphRequest struct {
	Starts       []string `json:"starts,omitempty"`
	Ends         []string `json:"ends"`
	MaxDepth     int      `json:"max_depth,omitempty"`
	MaxRecipes   int      `json:"max_recipes,omitempty"`
	MaxResources int      `json:"max_resources,omitempty"`
}
type QuestQueryRequest struct {
	ID        string   `json:"id,omitempty"`
	Query     string   `json:"query,omitempty"`
	ChapterID string   `json:"chapter_id,omitempty"`
	States    []string `json:"states,omitempty"`
	Offset    int      `json:"offset,omitempty"`
	Limit     int      `json:"limit,omitempty"`
}
type StatusRequest struct{}
type PlayerRequest struct {
	TooltipMaxLines int `json:"tooltip_max_lines,omitempty" jsonschema:"additional tooltip lines per inventory item, from 0 to 10"`
}
type PlayerSlotRequest struct {
	Scope string `json:"scope" jsonschema:"inventory scope: main or armor"`
	Slot  int    `json:"slot" jsonschema:"zero-based slot number"`
}
