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

    public void describe(RecipeHandlerRef ref, Map<String, Object> info) {
        if (!(ref.handler instanceof GTNEIDefaultHandler handler)) return;
        GTRecipe recipe = handler.getCache()
            .get(ref.recipeIndex).mRecipe;
        info.put("eut", recipe.mEUt);
        info.put("ticks", recipe.mDuration);
        info.put("tier", GTValues.VN[GTUtility.getTier(Math.abs((long) recipe.mEUt))]);
    }
}
