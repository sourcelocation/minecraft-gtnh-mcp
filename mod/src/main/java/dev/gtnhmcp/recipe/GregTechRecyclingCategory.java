package dev.gtnhmcp.recipe;

import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.Loader;
import gregtech.api.recipe.RecipeCategory;
import gregtech.nei.GTNEIDefaultHandler;

/**
 * GregTech tags its recycling recipes with a category ({@code gt.recipe.category.macerator_recycling} and so on).
 * Matching the name, not a list, picks up categories later GregTech versions add.
 */
public final class GregTechRecyclingCategory implements RecyclingRule {

    public boolean available() {
        return Loader.isModLoaded("gregtech");
    }

    public boolean recycles(RecipeHandlerRef ref) {
        if (!(ref.handler instanceof GTNEIDefaultHandler handler)) return false;
        if (!(handler.arecipes.get(ref.recipeIndex) instanceof GTNEIDefaultHandler.CachedDefaultRecipe cached))
            return false;
        RecipeCategory category = cached.mRecipe.getRecipeCategory();
        return category != null && category.unlocalizedName.contains("recycling");
    }
}
