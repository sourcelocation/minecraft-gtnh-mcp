package dev.gtnhmcp.recipe;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.Stack;

public record Plan(@JsonPropertyDescription("intermediates before the targets that need them") List<Step> steps,
    @JsonPropertyDescription("inputs no step makes: gather or supply these") List<Stack> raw,
    @JsonPropertyDescription("leftover outputs") List<Stack> surplus,
    @JsonPropertyDescription("why the plan may be incomplete") List<String> warnings) {

    public record Step(RecipeView recipe, long runs) {}
}
