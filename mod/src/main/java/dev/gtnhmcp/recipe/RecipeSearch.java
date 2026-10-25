package dev.gtnhmcp.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;

/** Finds an item's recipes with the sources policy applied: collapsed tabs counted, recycling left out. */
final class RecipeSearch {

    /** {@code collapsed}: recipe counts per collapsed machine; {@code recipes} streams the rest lazily. */
    record Result(Stream<RecipeHandlerRef> recipes, Map<String, Integer> collapsed) {}

    private final RecipeBook book;
    private final RecipeSources sources;

    RecipeSearch(RecipeBook book, RecipeSources sources) {
        this.book = book;
        this.sources = sources;
    }

    /**
     * @param machine  when set, only that machine's recipes, collapsed or not
     * @param recycled whether recycling recipes are included
     */
    Result find(ItemStack stack, boolean uses, String machine, boolean recycled) {
        List<IRecipeHandler> listed = new ArrayList<>();
        Map<String, Integer> collapsed = new LinkedHashMap<>();
        for (IRecipeHandler handler : book.handlers(stack, uses)) {
            String name = RecipeSources.machine(handler);
            if (machine != null) {
                if (name.equalsIgnoreCase(machine)) listed.add(handler);
            } else if (!recycled && sources.recycling(handler)) continue;
            else if (sources.collapsed(handler)) collapsed.merge(name, handler.numRecipes(), Integer::sum);
            else listed.add(handler);
        }
        return new Result(book.recipes(listed), collapsed);
    }
}
