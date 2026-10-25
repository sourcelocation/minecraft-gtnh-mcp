package dev.gtnhmcp.recipe;

import codechicken.nei.recipe.RecipeHandlerRef;

/** Recognises recipes that turn finished parts back into raw material. */
interface RecyclingRule {

    boolean recycles(RecipeHandlerRef recipe);
}
