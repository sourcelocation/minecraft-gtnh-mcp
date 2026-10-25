package dev.gtnhmcp.recipe;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.Stack;

public record RecipeView(@JsonPropertyDescription("pass to get_recipe; valid until the game restarts") String id,
    String machine, List<Ingredient> in, List<Ingredient> out,
    @JsonPropertyDescription("machine facts, e.g. GregTech eut, ticks, tier") Map<String, Object> info) {

    /**
     * One recipe slot: {@code chance} is the output probability, {@code alts} the number of interchangeable items, and
     * {@code kept} marks one that is not consumed, e.g. a circuit or mold; each is null when it does not apply.
     */
    public record Ingredient(Stack stack, Double chance, Integer alts, Boolean kept) {

        Ingredient withAmount(long amount) {
            return new Ingredient(new Stack(stack.item(), amount), chance, alts, kept);
        }
    }
}
