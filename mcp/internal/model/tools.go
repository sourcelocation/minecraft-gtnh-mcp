package model

import "encoding/json"

type CatalogRequest struct {
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
type GraphResponse struct {
	Envelope `json:"envelope"`
	Data     RecipeData `json:"data"`
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
type PlayerRequest struct{}
