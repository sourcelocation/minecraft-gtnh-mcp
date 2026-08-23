package dev.gtnhmcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.RecipeCatalysts;
import codechicken.nei.recipe.StackInfo;

/** Discovers and solves a bounded recipe chain directly against live NEI APIs. */
final class RecipePathProvider implements QueryProvider {

    private static final long DEFAULT_DEADLINE_NS = 1000000000L;
    private final RecipeProvider recipes;

    RecipePathProvider(RecipeProvider recipes) {
        this.recipes = recipes;
    }

    public JsonObject query(String method, JsonObject params) {
        return new Search(params).solve();
    }

    private final class Search {

        private final JsonObject params;
        private final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        private final int maxSteps;
        private final long deadline;
        private final Set<String> starts = new HashSet<String>();
        private final Set<String> excluded = new HashSet<String>();
        private final Set<String> inventory = new HashSet<String>();
        private final Set<String> active = new HashSet<String>();
        private final Map<String, JsonObject> machineMap = new HashMap<String, JsonObject>();
        private final Set<String> warningSet = new LinkedHashSet<String>();
        private final Map<String, Double> balance = new HashMap<String, Double>();
        private final JsonArray flows = new JsonArray();
        private boolean complete = true;

        Search(JsonObject params) {
            this.params = params;
            maxSteps = integer(params, "max_steps", 16, 1, 64);
            deadline = System.nanoTime() + DEFAULT_DEADLINE_NS;
            addSelectors(starts, params, "starts");
            addSelectors(excluded, params, "excludes_resources");
            if (player != null) {
                addInventory(player.inventory.mainInventory);
                addInventory(player.inventory.armorInventory);
            }
        }

        JsonObject solve() {
            if (!params.has("ends") || !params.get("ends")
                .isJsonArray()) return BridgeResponses.error("ends required");
            for (JsonElement end : params.getAsJsonArray("ends")) {
                List<ItemStack> resolved = recipes.resolveText(end.getAsString());
                if (resolved.isEmpty()) {
                    fail("resource not found: " + end.getAsString());
                    continue;
                }
                ItemStack target = normalized(resolved.get(0));
                String id = ResourceNormalizer.resourceID(target);
                addBalance(id, -1.0);
                produce(target);
            }

            JsonObject result = new JsonObject();
            result.add("flows", flows);
            JsonObject balances = new JsonObject();
            List<String> ids = new ArrayList<String>(balance.keySet());
            Collections.sort(ids);
            for (String id : ids) balances.addProperty(id, balance.get(id));
            result.add("balance", balances);
            JsonArray machines = new JsonArray();
            List<String> sortedMachines = new ArrayList<String>(machineMap.keySet());
            Collections.sort(sortedMachines);
            for (String machine : sortedMachines) machines.add(machineMap.get(machine));
            result.add("machines", machines);
            result.addProperty("complete", complete && noDeficit());
            if (!warningSet.isEmpty()) {
                JsonArray warnings = new JsonArray();
                for (String warning : warningSet) warnings.add(new JsonPrimitive(warning));
                result.add("warnings", warnings);
            }
            return result;
        }

        private void produce(ItemStack wanted) {
            String wantedID = ResourceNormalizer.resourceID(wanted);
            if (getBalance(wantedID) >= 0) return;
            if (starts.contains(wantedID)) {
                balance.put(wantedID, 0.0);
                return;
            }
            if (excluded.contains(wantedID)) {
                fail("excluded resource required: " + wantedID);
                return;
            }
            if (flows.size() >= maxSteps) {
                fail("recipe chain reached max_steps");
                return;
            }
            if (System.nanoTime() > deadline) {
                fail("recipe chain reached deadline");
                return;
            }
            if (!active.add(wantedID)) {
                fail("recipe cycle at " + wantedID);
                return;
            }
            try {
                Candidate selected = selectCandidate(wanted);
                if (selected == null) {
                    fail("no recipe produces " + wantedID);
                    return;
                }
                double produced = outputAmount(selected.recipe, wanted);
                if (produced <= 0) {
                    fail("recipe has no measurable output for " + wantedID);
                    return;
                }
                double runs = Math.ceil(-getBalance(wantedID) / produced);
                if (runs < 1) runs = 1;
                addFlow(selected, runs);

                for (Recipe.RecipeIngredient output : selected.recipe.getResults()) {
                    ItemStack chosen = output.contains(wanted) ? wanted : choose(output);
                    if (chosen == null) continue;
                    addBalance(ResourceNormalizer.resourceID(chosen), amount(output) * chance(output) * runs);
                }
                for (Recipe.RecipeIngredient input : selected.recipe.getIngredients()) {
                    if (input.getAmount() <= 0) continue;
                    ItemStack chosen = choose(input);
                    if (chosen == null) {
                        fail("recipe ingredient has no usable permutation");
                        continue;
                    }
                    String inputID = ResourceNormalizer.resourceID(chosen);
                    addBalance(inputID, -amount(input) * runs);
                    produce(chosen);
                }
            } finally {
                active.remove(wantedID);
            }
        }

        private Candidate selectCandidate(ItemStack wanted) {
            List<Candidate> candidates = new ArrayList<Candidate>();
            ItemStack query = normalized(wanted);
            for (IRecipeHandler handler : GuiCraftingRecipe.getCraftingHandlers("item", query)) {
                for (int i = 0; i < handler.numRecipes(); i++) {
                    if (System.nanoTime() > deadline) return null;
                    Recipe recipe = Recipe.of(handler, i);
                    if (recipe == null || recipe.getRecipeId() == null
                        || outputAmount(recipe, wanted) <= 0
                        || requiresExcluded(recipe)) continue;
                    candidates.add(new Candidate(handler, i, recipe, score(recipe)));
                }
            }
            Collections.sort(candidates, new Comparator<Candidate>() {

                public int compare(Candidate left, Candidate right) {
                    int score = right.score - left.score;
                    return score != 0 ? score : left.id.compareTo(right.id);
                }
            });
            return candidates.isEmpty() ? null : candidates.get(0);
        }

        private int score(Recipe recipe) {
            int score = 0;
            for (Recipe.RecipeIngredient ingredient : recipe.getIngredients()) {
                ItemStack chosen = choose(ingredient);
                if (chosen == null) return Integer.MIN_VALUE;
                String id = ResourceNormalizer.resourceID(chosen);
                if (starts.contains(id)) score += 4;
                else if (inventory.contains(id)) score += 2;
            }
            return score;
        }

        private boolean requiresExcluded(Recipe recipe) {
            for (Recipe.RecipeIngredient ingredient : recipe.getIngredients()) {
                boolean usable = false;
                for (ItemStack alternative : ingredient.getPermutations()) {
                    if (!excluded.contains(ResourceNormalizer.resourceID(normalized(alternative)))) {
                        usable = true;
                        break;
                    }
                }
                if (!usable) return true;
            }
            return false;
        }

        private ItemStack choose(Recipe.RecipeIngredient ingredient) {
            ItemStack best = null;
            int bestScore = Integer.MIN_VALUE;
            String bestID = null;
            for (ItemStack alternative : ingredient.getPermutations()) {
                ItemStack candidate = normalized(alternative);
                String id = ResourceNormalizer.resourceID(candidate);
                if (excluded.contains(id)) continue;
                int score = starts.contains(id) ? 2 : inventory.contains(id) ? 1 : 0;
                if (best == null || score > bestScore || (score == bestScore && id.compareTo(bestID) < 0)) {
                    best = candidate;
                    bestScore = score;
                    bestID = id;
                }
            }
            return best;
        }

        private double outputAmount(Recipe recipe, ItemStack wanted) {
            if (recipe.getResult(wanted) == null) return 0;
            double result = 0;
            for (Recipe.RecipeIngredient output : recipe.getResults())
                if (output.contains(wanted)) result += amount(output) * chance(output);
            return result;
        }

        private void addFlow(Candidate selected, double runs) {
            JsonObject flow = new JsonObject();
            flow.addProperty("recipe_id", recipes.recipeID(selected.recipe));
            flow.addProperty("handler_id", selected.handler.getHandlerId());
            flow.addProperty("handler_name", selected.recipe.getHandlerName());
            JsonObject machine = machine(selected.handler);
            if (machine != null) {
                flow.add("machine", machine);
                machineMap.put(
                    machine.get("id")
                        .getAsString(),
                    machine);
            }
            flow.addProperty("runs", runs);
            flow.add("inputs", recipes.groups(selected.recipe.getIngredients(), true));
            flow.add("outputs", recipes.groups(selected.recipe.getResults(), false));
            enrichGT(flow, selected);
            flows.add(flow);
        }

        private JsonObject machine(IRecipeHandler handler) {
            List<PositionedStack> catalysts = RecipeCatalysts.getRecipeCatalysts(handler);
            if (catalysts != null)
                for (PositionedStack catalyst : catalysts) if (catalyst != null && catalyst.item != null)
                    return ResourceNormalizer.resource(catalyst.item, 1, player, 2);
            return null;
        }

        private void enrichGT(JsonObject flow, Candidate selected) {
            if (!(selected.handler instanceof gregtech.nei.GTNEIDefaultHandler)) return;
            gregtech.nei.GTNEIDefaultHandler handler = (gregtech.nei.GTNEIDefaultHandler) selected.handler;
            if (handler.getRecipeMap() != null) flow.addProperty("recipe_map", handler.getRecipeMap().unlocalizedName);
            try {
                gregtech.api.util.GTRecipe recipe = handler.getCache()
                    .get(selected.index).mRecipe;
                flow.addProperty("eut", recipe.mEUt);
                flow.addProperty("duration_ticks", recipe.mDuration);
                flow.addProperty("total_eu", (long) recipe.mEUt * recipe.mDuration);
                flow.addProperty("nbt_sensitive", recipe.isNBTSensitive);
            } catch (RuntimeException ignored) {}
        }

        private void addSelectors(Set<String> target, JsonObject source, String name) {
            if (!source.has(name) || !source.get(name)
                .isJsonArray()) return;
            for (JsonElement value : source.getAsJsonArray(name)) {
                String text = value.getAsString();
                List<ItemStack> resolved = recipes.resolveText(text);
                if (resolved.isEmpty()) target.add(text);
                else for (ItemStack stack : resolved) target.add(ResourceNormalizer.resourceID(normalized(stack)));
            }
        }

        private void addInventory(ItemStack[] stacks) {
            for (ItemStack stack : stacks)
                if (stack != null) inventory.add(ResourceNormalizer.resourceID(normalized(stack)));
        }

        private ItemStack normalized(ItemStack stack) {
            ItemStack normalized = StackInfo.normalizeRecipeQueryStack(stack.copy());
            return normalized == null ? stack.copy() : normalized;
        }

        private int amount(Recipe.RecipeIngredient ingredient) {
            return ingredient.getAmount() > 0 ? ingredient.getAmount() : 1;
        }

        private double chance(Recipe.RecipeIngredient ingredient) {
            return ingredient.getChance() <= 0 ? 1.0 : ingredient.getChance() / 10000.0;
        }

        private double getBalance(String id) {
            Double value = balance.get(id);
            return value == null ? 0 : value;
        }

        private void addBalance(String id, double amount) {
            balance.put(id, getBalance(id) + amount);
        }

        private boolean noDeficit() {
            for (Map.Entry<String, Double> entry : balance.entrySet())
                if (entry.getValue() < 0 && !starts.contains(entry.getKey())) return false;
            return true;
        }

        private void fail(String warning) {
            complete = false;
            warningSet.add(warning);
        }
    }

    private static int integer(JsonObject params, String name, int defaultValue, int min, int max) {
        if (!params.has(name)) return defaultValue;
        try {
            int value = params.get(name)
                .getAsInt();
            return value < min || value > max ? defaultValue : value;
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static final class Candidate {

        final IRecipeHandler handler;
        final int index;
        final Recipe recipe;
        final int score;
        final String id;

        Candidate(IRecipeHandler handler, int index, Recipe recipe, int score) {
            this.handler = handler;
            this.index = index;
            this.recipe = recipe;
            this.score = score;
            id = recipe.getRecipeId()
                .toJsonObject()
                .toString();
        }
    }
}
