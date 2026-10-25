package dev.gtnhmcp.recipe;

import java.util.List;
import java.util.Set;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;
import dev.gtnhmcp.core.Text;

/** Which NEI tabs are listed in full, which only as a count, and which recipes are recycling. */
final class RecipeSources {

    /**
     * Listed as a count unless asked for by machine: rare or end-game sources, and tabs that mostly repeat another
     * (GT++'s multiblock copies of the single-block maps; Et Futurum's Blasting, a subset of Smelting).
     */
    private static final Set<String> COLLAPSED = Set.of(
        "mobsinfo.mobhandler",
        "gt.recipe.eyeofharmony",
        "gt.recipe.spaceMining",
        "gt.recipe.multicentrifuge",
        "gt.recipe.multielectro",
        "gt.recipe.multimixer",
        "gt.recipe.multidehydrator",
        "etfuturum.blastfurnace");

    private final List<RecyclingRule> recycling;

    RecipeSources(List<RecyclingRule> recycling) {
        this.recycling = List.copyOf(recycling);
    }

    boolean collapsed(IRecipeHandler handler) {
        String id = id(handler);
        return id != null && COLLAPSED.contains(id);
    }

    /** NEI's own handler id: the overlay identifier, else the handler id; some handlers have neither. */
    private static String id(IRecipeHandler handler) {
        String overlay = handler.getOverlayIdentifier();
        return overlay != null ? overlay : handler.getHandlerId();
    }

    boolean recycling(RecipeHandlerRef recipe) {
        return recycling.stream()
            .anyMatch(rule -> rule.recycles(recipe));
    }

    /** The tab's name, as {@code machine} in every recipe. */
    static String machine(IRecipeHandler handler) {
        return Text.clean(handler.getRecipeName());
    }
}
