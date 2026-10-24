package dev.gtnhmcp.recipe;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.Recipe.RecipeIngredient;
import dev.gtnhmcp.core.Items;

/** Avoids excluded items and prefers recipes whose inputs are supplied or already in the inventory. */
final class PreferAvailable implements PlanPolicy {

    private final Items items;
    private final Set<String> have;
    private final Set<String> avoid;
    private final Set<String> inventory;

    PreferAvailable(Items items, Set<String> have, Set<String> avoid, Set<String> inventory) {
        this.items = items;
        this.have = have;
        this.avoid = avoid;
        this.inventory = inventory;
    }

    public boolean supplied(String id) {
        return have.contains(id);
    }

    public Optional<Candidate> choose(List<Candidate> candidates) {
        return candidates.stream()
            .filter(this::allowed)
            .max(Comparator.comparingInt(this::score));
    }

    /** A supplied or owned permutation, else NEI's displayed one, else any that is not avoided. */
    public ItemStack pick(RecipeIngredient ingredient) {
        ItemStack shown = ingredient.getItemStack();
        return ingredient.getPermutations()
            .stream()
            .filter(s -> !avoid.contains(items.id(s)))
            .max(
                Comparator.comparingInt((ItemStack s) -> rank(items.id(s)))
                    .thenComparing(
                        s -> items.id(s)
                            .equals(items.id(shown))))
            .orElse(null);
    }

    /** Rejects recipes needing an avoided item, and loops that consume an output (cell filling, unpacking). */
    private boolean allowed(Candidate c) {
        Set<String> outputs = c.recipe()
            .getResults()
            .stream()
            .flatMap(
                r -> r.getPermutations()
                    .stream())
            .map(items::id)
            .collect(Collectors.toSet());
        return c.recipe()
            .getIngredients()
            .stream()
            .filter(i -> i.getAmount() > 0)
            .allMatch(i -> {
                ItemStack stack = pick(i);
                return stack != null && !outputs.contains(items.id(stack));
            });
    }

    private int score(Candidate c) {
        int score = -c.recipe()
            .getIngredients()
            .size();
        for (RecipeIngredient ingredient : c.recipe()
            .getIngredients()) {
            ItemStack stack = pick(ingredient);
            if (stack != null) score += 4 * rank(items.id(stack));
        }
        return score;
    }

    private int rank(String id) {
        return have.contains(id) ? 2 : inventory.contains(id) ? 1 : 0;
    }
}
