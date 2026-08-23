package dev.gtnhmcp;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import codechicken.nei.ItemList;
import codechicken.nei.SearchField;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;

/** NEI and GregTech recipe queries. */
public final class RecipeProvider implements QueryProvider {

    private static final int MAX_ALTERNATIVES = 5;

    private static final class CompactGroups {

        private final JsonArray groups;
        private final boolean truncated;

        private CompactGroups(JsonArray groups, boolean truncated) {
            this.groups = groups;
            this.truncated = truncated;
        }
    }

    private final java.util.Map<String, Recipe> recipesByID = new java.util.LinkedHashMap<String, Recipe>() {

        protected boolean removeEldestEntry(java.util.Map.Entry<String, Recipe> eldest) {
            return size() > 1000;
        }
    };

    public JsonObject query(String method, JsonObject params) {
        if ("item_lookup".equals(method)) return itemLookup(params);
        if ("lookup_recipes".equals(method)) return lookup(params, true);
        if ("lookup_uses".equals(method)) return lookup(params, false);
        if ("lookup_recipe".equals(method)) return lookupRecipe(params);
        throw new IllegalArgumentException("unknown recipe method: " + method);
    }

    private JsonObject itemLookup(JsonObject p) {
        if (!ItemList.loadFinished || ItemList.items == null) return BridgeResponses.error("NEI item list loading");
        String q = p.has("query") ? p.get("query")
            .getAsString() : "";
        int offset = integer(p, "offset", 0, 0, 100000), limit = integer(p, "limit", 50, 1, 200), seen = 0;
        ItemFilter filter = SearchField.getFilter(q);
        JsonArray rows = new JsonArray();
        boolean more = false;
        for (ItemStack s : ItemList.items) {
            if (!filter.matches(s)) continue;
            if (seen++ < offset) continue;
            if (rows.size() >= limit) {
                more = true;
                break;
            }
            rows.add(ResourceNormalizer.resource(s, s.stackSize, player(), 2));
        }
        JsonObject d = new JsonObject();
        d.add("items", rows);
        d.addProperty("offset", offset);
        d.addProperty("limit", limit);
        d.addProperty("next_offset", offset + rows.size());
        d.addProperty("truncated", more);
        return d;
    }

    private JsonObject lookup(JsonObject p, boolean recipes) {
        if (!p.has("id") || !p.get("id")
            .isJsonPrimitive()) return BridgeResponses.error("id required");
        JsonObject selector = new JsonObject();
        selector.addProperty(
            "id",
            p.get("id")
                .getAsString());
        java.util.List<ItemStack> focus = resolve(selector);
        if (focus.isEmpty()) return BridgeResponses.error("id not found");
        int offset = integer(p, "offset", 0, 0, 100000), limit = integer(p, "limit", 50, 1, 200), matched = 0,
            returned = 0;
        JsonArray handlers = new JsonArray();
        java.util.LinkedHashMap<String, JsonObject> handlerGroups = new java.util.LinkedHashMap<String, JsonObject>();
        java.util.HashSet<String> recipeKeys = new java.util.HashSet<String>();
        boolean more = false;
        outer: for (ItemStack s : focus) {
            java.util.List<? extends IRecipeHandler> hs = recipes
                ? GuiCraftingRecipe.getCraftingHandlers("item", s.copy())
                : GuiUsageRecipe.getUsageHandlers("item", s.copy());
            for (IRecipeHandler h : hs) {
                JsonObject group = null;
                for (int i = 0; i < h.numRecipes(); i++) {
                    Recipe nativeRecipe = Recipe.of(h, i);
                    JsonObject r = normalize(
                        nativeRecipe,
                        recipes ? null
                            : p.get("id")
                                .getAsString(),
                        recipes ? p.get("id")
                            .getAsString() : null,
                        true);
                    if (r == null || !recipeKeys.add(
                        r.get("recipe_id")
                            .getAsString()))
                        continue;
                    if (matched++ < offset) continue;
                    if (returned >= limit) {
                        more = true;
                        break outer;
                    }
                    if (group == null) {
                        String key = h.getHandlerId() + "\u0000"
                            + r.get("handler_name")
                                .getAsString();
                        group = handlerGroups.get(key);
                        if (group == null) {
                            group = new JsonObject();
                            group.addProperty("handler_id", h.getHandlerId());
                            group.add("handler_name", r.remove("handler_name"));
                            group.add("recipes", new JsonArray());
                            handlerGroups.put(key, group);
                            handlers.add(group);
                        } else r.remove("handler_name");
                    }
                    group.getAsJsonArray("recipes")
                        .add(r);
                    returned++;
                }
            }
        }
        JsonObject d = new JsonObject();
        d.add("handlers", handlers);
        d.addProperty("truncated", more);
        d.addProperty("offset", offset);
        d.addProperty("next_offset", offset + returned);
        return d;
    }

    java.util.List<ItemStack> resolve(JsonObject s) {
        java.util.ArrayList<ItemStack> out = new java.util.ArrayList<ItemStack>();
        if (!ItemList.loadFinished || ItemList.items == null) return out;
        String id = s.has("id") ? s.get("id")
            .getAsString() : null;
        String q = s.has("query") ? s.get("query")
            .getAsString() : null;
        if (id == null && q == null) return out;
        codechicken.nei.api.ItemFilter f = q == null ? null : SearchField.getFilter(q);
        for (ItemStack x : ItemList.items) {
            if ((id != null && !id.equals(ResourceNormalizer.resourceID(x))) || (f != null && !f.matches(x))) continue;
            out.add(x);
            if (out.size() == 3) break;
        }
        return out;
    }

    java.util.List<ItemStack> resolveText(String text) {
        JsonObject s = new JsonObject();
        s.addProperty("id", text);
        java.util.List<ItemStack> r = resolve(s);
        if (r.isEmpty()) {
            s.remove("id");
            s.addProperty("query", text);
            r = resolve(s);
        }
        return r;
    }

    private JsonObject lookupRecipe(JsonObject p) {
        if (!p.has("recipe_id") || !p.get("recipe_id")
            .isJsonPrimitive()) return BridgeResponses.error("recipe_id required");
        Recipe recipe = recipesByID.get(
            p.get("recipe_id")
                .getAsString());
        if (recipe == null) return BridgeResponses.error("recipe_id not found; run a recipe or usage lookup first");
        JsonObject result = new JsonObject();
        result.add("recipe", normalize(recipe, null, null, false));
        return result;
    }

    private JsonObject normalize(Recipe r, String preferredInput, String preferredOutput, boolean truncate) {
        if (r == null) return null;
        Recipe.RecipeId recipeID;
        try {
            recipeID = r.getRecipeId();
        } catch (RuntimeException ignored) {
            return null;
        }
        if (recipeID == null) return null;
        JsonObject o = new JsonObject();
        o.addProperty("handler_name", r.getHandlerName());
        String id = shortRecipeID(recipeID);
        recipesByID.put(id, r);
        o.addProperty("recipe_id", id);
        CompactGroups inputs = compactGroups(r.getIngredients(), preferredInput, truncate);
        CompactGroups outputs = compactGroups(r.getResults(), preferredOutput, truncate);
        o.add("inputs", inputs.groups);
        if (inputs.truncated) o.addProperty("inputs_truncated", true);
        o.add("outputs", outputs.groups);
        if (outputs.truncated) o.addProperty("outputs_truncated", true);
        return o;
    }

    String recipeID(Recipe recipe) {
        if (recipe == null || recipe.getRecipeId() == null) throw new IllegalArgumentException("recipe has no NEI id");
        String id = shortRecipeID(recipe.getRecipeId());
        recipesByID.put(id, recipe);
        return id;
    }

    private String shortRecipeID(Recipe.RecipeId recipeID) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(
                    recipeID.toJsonObject()
                        .toString()
                        .getBytes("UTF-8"));
            StringBuilder result = new StringBuilder("r_");
            for (int i = 0; i < 12; i++) {
                int value = digest[i] & 0xff;
                if (value < 16) result.append('0');
                result.append(Integer.toHexString(value));
            }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 unavailable", e);
        }
    }

    private CompactGroups compactGroups(java.util.List<Recipe.RecipeIngredient> ingredients, String preferredID,
        boolean truncate) {
        JsonArray result = new JsonArray();
        JsonObject preferredGroup = null;
        for (Recipe.RecipeIngredient ingredient : ingredients) {
            JsonArray alternatives = new JsonArray();
            boolean truncated = false;
            double chance = chance(ingredient);
            ResourceNormalizer.ItemFormat format = ResourceNormalizer.ItemFormat.compact(2)
                .withChance(chance);
            ItemStack preferred = null;
            if (preferredID != null) for (ItemStack stack : ingredient.getPermutations()) {
                if (preferredID.equals(ResourceNormalizer.resourceID(stack))) {
                    preferred = stack;
                    alternatives.add(ResourceNormalizer.resource(stack, ingredient.getAmount(), player(), format));
                    break;
                }
            }
            boolean skippedPreferred = false;
            for (ItemStack stack : ingredient.getPermutations()) {
                if (!skippedPreferred && preferred != null
                    && preferredID.equals(ResourceNormalizer.resourceID(stack))) {
                    skippedPreferred = true;
                    continue;
                }
                if (truncate && alternatives.size() >= MAX_ALTERNATIVES) {
                    truncated = true;
                    break;
                }
                alternatives.add(ResourceNormalizer.resource(stack, ingredient.getAmount(), player(), format));
            }
            JsonObject group;
            if (alternatives.size() == 1) group = alternatives.get(0)
                .getAsJsonObject();
            else {
                group = new JsonObject();
                group.add("alternatives", alternatives);
                if (truncated) group.addProperty("truncated", true);
            }
            if (preferred != null && preferredGroup == null) preferredGroup = group;
            else result.add(group);
        }
        if (preferredGroup != null) {
            JsonArray ordered = new JsonArray();
            ordered.add(preferredGroup);
            for (com.google.gson.JsonElement group : result) ordered.add(group);
            result = ordered;
        }
        if (!truncate || result.size() <= MAX_ALTERNATIVES) return new CompactGroups(result, false);
        JsonArray limited = new JsonArray();
        for (int i = 0; i < MAX_ALTERNATIVES; i++) limited.add(result.get(i));
        return new CompactGroups(limited, true);
    }

    JsonArray groups(java.util.List<Recipe.RecipeIngredient> in, boolean consumed) {
        JsonArray a = new JsonArray();
        for (Recipe.RecipeIngredient g : in) {
            JsonArray alts = new JsonArray();
            ResourceNormalizer.ItemFormat format = ResourceNormalizer.ItemFormat.compact(2)
                .withChance(chance(g));
            for (ItemStack s : g.getPermutations())
                alts.add(ResourceNormalizer.resource(s, g.getAmount(), player(), format));
            JsonObject x;
            if (alts.size() == 1) x = alts.get(0)
                .getAsJsonObject();
            else {
                x = new JsonObject();
                x.add("alternatives", alts);
            }
            x.addProperty("consumed", consumed && g.getAmount() > 0);
            x.addProperty("catalyst", consumed && g.getAmount() <= 0);
            a.add(x);
        }
        return a;
    }

    private double chance(Recipe.RecipeIngredient ingredient) {
        return ingredient.getChance() == 0 ? 1.0 : ingredient.getChance() / 10000.0;
    }

    private EntityPlayer player() {
        return Minecraft.getMinecraft().thePlayer;
    }

    private int integer(JsonObject p, String name, int def, int lo, int hi) {
        if (!p.has(name)) return def;
        try {
            int v = p.get(name)
                .getAsInt();
            return v < lo || v > hi ? def : v;
        } catch (Exception e) {
            return def;
        }
    }

}
