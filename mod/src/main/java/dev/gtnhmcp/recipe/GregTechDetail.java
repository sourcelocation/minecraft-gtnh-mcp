package dev.gtnhmcp.recipe;

import java.util.Map;

import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.Loader;
import gregtech.api.enums.GTValues;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.nei.GTNEIDefaultHandler;

public final class GregTechDetail implements RecipeDetail {

    public boolean available() {
        return Loader.isModLoaded("gregtech");
    }

    /** The index refers to this lookup's results ({@code arecipes}), not GT's cache of the whole recipe map. */
    public void describe(RecipeHandlerRef ref, Map<String, Object> info) {
        if (!(ref.handler instanceof GTNEIDefaultHandler handler)) return;
        if (!(handler.arecipes.get(ref.recipeIndex) instanceof GTNEIDefaultHandler.CachedDefaultRecipe cached)) return;
        GTRecipe recipe = cached.mRecipe;
        info.put("eut", recipe.mEUt);
        info.put("ticks", recipe.mDuration);
        info.put("tier", GTValues.VN[GTUtility.getTier(Math.abs((long) recipe.mEUt))]);
    }
}
