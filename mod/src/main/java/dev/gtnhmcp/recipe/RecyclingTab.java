package dev.gtnhmcp.recipe;

import java.util.Locale;

import codechicken.nei.recipe.RecipeHandlerRef;

/** Whole NEI tabs of recycling, which say so in their name, e.g. GregTech's "Macerator Recycling". */
final class RecyclingTab implements RecyclingRule {

    public boolean recycles(RecipeHandlerRef recipe) {
        return RecipeSources.machine(recipe.handler)
            .toLowerCase(Locale.ROOT)
            .contains("recycling");
    }
}
