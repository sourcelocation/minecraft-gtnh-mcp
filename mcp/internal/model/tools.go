package model

import "encoding/json"

type ItemLookupRequest struct {
	Query  string `json:"query"`
	Offset int    `json:"offset,omitempty"`
	Limit  int    `json:"limit,omitempty"`
}
type RecipeLookupRequest struct {
	Resource ResourceSelector `json:"resource"`
	Offset   int              `json:"offset,omitempty"`
	Limit    int              `json:"limit,omitempty"`
}
type ResourceSelector struct {
	ID    string `json:"id,omitempty"`
	Query string `json:"query,omitempty"`
}
type RecipeGetRequest struct {
	ID json.RawMessage `json:"id"`
}
type RecipePathsRequest struct {
	Starts            []string `json:"starts,omitempty"`
	Ends              []string `json:"ends"`
	ExcludesResources []string `json:"excludes_resources,omitempty"`
	MaxSteps          int      `json:"max_steps,omitempty"`
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
