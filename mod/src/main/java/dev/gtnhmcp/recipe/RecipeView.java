package dev.gtnhmcp.recipe;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.Stack;

public record RecipeView(@JsonPropertyDescription("pass to get_recipe; valid until the game restarts") String id,
    String machine, List<Ingredient> in, List<Ingredient> out,
    @JsonPropertyDescription("machine facts, e.g. GregTech eut, ticks, tier") Map<String, Object> info) {

    /**
     * One recipe slot: {@code chance} is its probability (1 when certain), {@code alts} how many items fit it, and
     * {@code kept} marks one that is not consumed, e.g. a circuit or mold.
     */
    public record Ingredient(Stack stack, double chance, int alts, boolean kept) {

        Ingredient withAmount(long amount) {
            return new Ingredient(new Stack(stack.item(), amount), chance, alts, kept);
        }
    }
}
