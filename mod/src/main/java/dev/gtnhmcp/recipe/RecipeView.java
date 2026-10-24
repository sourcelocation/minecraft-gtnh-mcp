package dev.gtnhmcp.recipe;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record RecipeView(@JsonPropertyDescription("pass to get_recipe; valid until the game restarts") String id,
    String machine, List<Ingredient> in, List<Ingredient> out,
    @JsonPropertyDescription("machine facts, e.g. GregTech eut, ticks, tier") Map<String, Object> info) {

    public record Ingredient(String id, String name, long amount,
        @JsonPropertyDescription("output probability, omitted when 1") Double chance,
        @JsonPropertyDescription("number of interchangeable items, omitted when 1") Integer alts,
        @JsonPropertyDescription("true when not consumed, e.g. a circuit or mold") Boolean kept) {

        Ingredient withAmount(long amount) {
            return new Ingredient(id, name, amount, chance, alts, kept);
        }
    }
}
