package dev.gtnhmcp.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;

import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.Recipe.RecipeIngredient;
import codechicken.nei.recipe.RecipeHandlerRef;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Stack;
import dev.gtnhmcp.recipe.PlanPolicy.Candidate;

/** Chains recipes backwards from targets to raw materials. Amounts are expected values for chanced outputs. */
final class ChainPlanner {

    private static final long BUDGET_NANOS = 750_000_000L;
    private static final int MAX_CANDIDATES = 64;

    private final Items items;
    private final RecipeSearch search;
    private final RecipeRenderer renderer;

    ChainPlanner(Items items, RecipeSearch search, RecipeRenderer renderer) {
        this.items = items;
        this.search = search;
        this.renderer = renderer;
    }

    Plan plan(Map<ItemStack, Long> targets, PlanPolicy policy, int maxSteps) {
        return new Run(policy, maxSteps).solve(targets);
    }

    private final class Run {

        private final PlanPolicy policy;
        private final int maxSteps;
        private final long deadline = System.nanoTime() + BUDGET_NANOS;
        private final Map<String, Double> balance = new LinkedHashMap<>();
        private final Map<String, ItemStack> stacks = new HashMap<>();
        private final Map<RecipeHandlerRef, Long> runs = new LinkedHashMap<>();
        private final Set<String> active = new HashSet<>();
        private final Set<String> warnings = new LinkedHashSet<>();

        Run(PlanPolicy policy, int maxSteps) {
            this.policy = policy;
            this.maxSteps = maxSteps;
        }

        Plan solve(Map<ItemStack, Long> targets) {
            targets.forEach((stack, amount) -> {
                add(stack, -amount);
                produce(stack);
            });
            List<Plan.Step> steps = new ArrayList<>();
            runs.forEach((ref, n) -> steps.add(0, new Plan.Step(renderer.render(ref), n)));
            List<Stack> raw = new ArrayList<>(), surplus = new ArrayList<>();
            balance.forEach((id, amount) -> {
                if (amount < -1e-9) raw.add(items.stack(stacks.get(id), (long) Math.ceil(-amount)));
                else if (amount >= 1) surplus.add(items.stack(stacks.get(id), (long) Math.floor(amount)));
            });
            return new Plan(steps, raw, surplus, new ArrayList<>(warnings));
        }

        private void produce(ItemStack wanted) {
            String id = items.id(wanted);
            double need = -balance.getOrDefault(id, 0.0);
            if (need <= 0 || policy.supplied(id)) return;
            if (runs.size() >= maxSteps) {
                warnings.add("max_steps reached");
                return;
            }
            if (System.nanoTime() > deadline) {
                warnings.add("time budget reached");
                return;
            }
            if (!active.add(id)) {
                warnings.add("cycle at " + id);
                return;
            }
            try {
                Candidate chosen = policy.choose(candidates(wanted))
                    .orElse(null);
                if (chosen == null) return;
                long n = (long) Math.ceil(need / made(chosen.recipe(), wanted));
                runs.merge(chosen.ref(), n, Long::sum);
                for (RecipeIngredient out : chosen.recipe()
                    .getResults()) {
                    ItemStack stack = out.contains(wanted) ? wanted : policy.pick(out);
                    if (stack != null) add(stack, out.getAmount() * RecipeRenderer.chance(out) * n);
                }
                for (RecipeIngredient in : chosen.recipe()
                    .getIngredients()) {
                    if (in.getAmount() <= 0) continue;
                    ItemStack stack = policy.pick(in);
                    add(stack, -(double) in.getAmount() * n);
                    produce(stack);
                }
            } finally {
                active.remove(id);
            }
        }

        private List<Candidate> candidates(ItemStack wanted) {
            return search.find(wanted, false, null, false)
                .recipes()
                .limit(MAX_CANDIDATES)
                .map(ref -> new Candidate(ref, Recipe.of(ref)))
                .filter(c -> c.recipe() != null && made(c.recipe(), wanted) > 0)
                .toList();
        }

        private void add(ItemStack stack, double amount) {
            String id = items.id(stack);
            stacks.putIfAbsent(id, stack);
            balance.merge(id, amount, Double::sum);
        }

        private double made(Recipe recipe, ItemStack wanted) {
            return recipe.getResults()
                .stream()
                .filter(out -> out.contains(wanted))
                .mapToDouble(out -> out.getAmount() * RecipeRenderer.chance(out))
                .sum();
        }
    }
}
