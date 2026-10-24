package dev.gtnhmcp.recipe;

import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.Recipe.RecipeIngredient;
import codechicken.nei.recipe.RecipeHandlerRef;

/** The choices a chain plan makes; swap the implementation to change planning without touching the planner. */
interface PlanPolicy {

    record Candidate(RecipeHandlerRef ref, Recipe recipe) {}

    /** True when the item is supplied, so the plan stops there. */
    boolean supplied(String id);

    /** The recipe to make an item with, or empty to treat it as a raw material. */
    Optional<Candidate> choose(List<Candidate> candidates);

    /** The permutation to use for an ingredient, or null when none is allowed. */
    ItemStack pick(RecipeIngredient ingredient);
}
