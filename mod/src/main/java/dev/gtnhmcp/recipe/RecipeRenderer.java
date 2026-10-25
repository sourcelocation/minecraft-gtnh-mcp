package dev.gtnhmcp.recipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.Recipe.RecipeIngredient;
import codechicken.nei.recipe.RecipeHandlerRef;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Text;

/** Turns NEI recipes into compact views; machine facts come from every available {@link RecipeDetail}. */
final class RecipeRenderer {

    private final Items items;
    private final RecipeBook book;
    private final List<RecipeDetail> details;

    RecipeRenderer(Items items, RecipeBook book, List<RecipeDetail> details) {
        this.items = items;
        this.book = book;
        this.details = details;
    }

    RecipeView render(RecipeHandlerRef ref) {
        Recipe recipe = Recipe.of(ref);
        Map<String, Object> info = new LinkedHashMap<>();
        for (RecipeDetail detail : details) detail.describe(ref, info);
        return new RecipeView(
            book.handle(recipe),
            Text.clean(ref.handler.getRecipeName()),
            ingredients(recipe.getIngredients()),
            ingredients(recipe.getResults()),
            info);
    }

    /** One entry per distinct ingredient: a crafting grid's repeated slots become one amount. */
    private List<RecipeView.Ingredient> ingredients(List<RecipeIngredient> list) {
        Map<RecipeView.Ingredient, Long> merged = new LinkedHashMap<>();
        for (RecipeIngredient ingredient : list) {
            RecipeView.Ingredient view = ingredient(ingredient);
            merged.merge(
                view.withAmount(0),
                view.stack()
                    .amount(),
                Long::sum);
        }
        return merged.entrySet()
            .stream()
            .map(
                e -> e.getKey()
                    .withAmount(e.getValue()))
            .toList();
    }

    private RecipeView.Ingredient ingredient(RecipeIngredient ingredient) {
        ItemStack stack = ingredient.getItemStack();
        boolean kept = ingredient.getAmount() <= 0;
        int alts = ingredient.getPermutations()
            .size();
        return new RecipeView.Ingredient(
            items.stack(stack, kept ? Math.max(1, stack.stackSize) : ingredient.getAmount()),
            chance(ingredient),
            alts > 1 ? alts : null,
            kept ? Boolean.TRUE : null);
    }

    static Double chance(RecipeIngredient ingredient) {
        int chance = ingredient.getChance();
        return chance > 0 && chance < 10000 ? chance / 10000.0 : null;
    }
}
