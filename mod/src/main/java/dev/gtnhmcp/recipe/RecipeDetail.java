package dev.gtnhmcp.recipe;

import java.util.Map;

import codechicken.nei.recipe.RecipeHandlerRef;
import dev.gtnhmcp.core.Extension;

/** Adds machine-specific facts to recipes it recognises; register in {@code META-INF/services}. */
public interface RecipeDetail extends Extension {

    void describe(RecipeHandlerRef recipe, Map<String, Object> info);
}
