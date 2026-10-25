package dev.gtnhmcp.recipe;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.Recipe.RecipeId;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.StackInfo;
import dev.gtnhmcp.core.ToolException;

/** NEI recipe lookups, plus short handles for NEI's (large) recipe ids. */
final class RecipeBook {

    private static final int MAX_HANDLES = 10_000;
    private final Map<String, RecipeId> handles = new LinkedHashMap<>(256, 0.75f, true) {

        protected boolean removeEldestEntry(Map.Entry<String, RecipeId> eldest) {
            return size() > MAX_HANDLES;
        }
    };

    /** NEI's tabs for recipes making the item, or using it. */
    List<? extends IRecipeHandler> handlers(ItemStack stack, boolean uses) {
        return uses ? GuiUsageRecipe.getUsageHandlers("item", query(stack))
            : GuiCraftingRecipe.getCraftingHandlers("item", query(stack));
    }

    /** The tabs' recipes, lazily, each recipe once. */
    Stream<RecipeHandlerRef> recipes(List<? extends IRecipeHandler> handlers) {
        Set<RecipeId> seen = new HashSet<>();
        return handlers.stream()
            .flatMap(
                h -> IntStream.range(0, h.numRecipes())
                    .mapToObj(i -> RecipeHandlerRef.of(h, i)))
            .filter(ref -> {
                RecipeId id = RecipeId.of(ref.handler, ref.recipeIndex);
                return id != null && seen.add(id);
            });
    }

    String handle(Recipe recipe) {
        RecipeId id = recipe.getRecipeId();
        String handle = Long.toUnsignedString(
            fnv(
                id.toJsonObject()
                    .toString()),
            36);
        handles.put(handle, id);
        return handle;
    }

    RecipeHandlerRef resolve(String handle) {
        RecipeId id = handles.get(handle);
        RecipeHandlerRef ref = id == null ? null : RecipeHandlerRef.of(id);
        if (ref == null)
            throw new ToolException("Unknown recipe id '" + handle + "'; look it up again with find_recipes");
        return ref;
    }

    private static ItemStack query(ItemStack stack) {
        ItemStack normalized = StackInfo.normalizeRecipeQueryStack(stack.copy());
        return normalized == null ? stack.copy() : normalized;
    }

    private static long fnv(String text) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < text.length(); i++) hash = (hash ^ text.charAt(i)) * 0x100000001b3L;
        return hash;
    }
}
