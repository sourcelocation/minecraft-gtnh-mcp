package dev.gtnhmcp.recipe;

import codechicken.nei.recipe.RecipeHandlerRef;
import dev.gtnhmcp.core.Extension;

/** Recognises recipes that turn finished parts back into raw material; register in {@code META-INF/services}. */
public interface RecyclingRule extends Extension {

    boolean recycles(RecipeHandlerRef recipe);
}
