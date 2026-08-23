package dev.gtnhmcp;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import codechicken.nei.ItemList;
import codechicken.nei.SearchField;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;

/** NEI and GregTech recipe queries. */
public final class RecipeProvider implements QueryProvider {

    public JsonObject query(String method, JsonObject params) {
        if ("item_lookup".equals(method)) return itemLookup(params);
        if ("recipe_lookup".equals(method)) return lookup(params);
        if ("recipes.graph".equals(method)) return graph(params);
        if ("recipe_get".equals(method)) return recipeGet(params);
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
            rows.add(ResourceNormalizer.identifiedResource(s, s.stackSize, player(), 2));
        }
        JsonObject d = new JsonObject();
        d.add("items", rows);
        d.addProperty("offset", offset);
        d.addProperty("limit", limit);
        d.addProperty("next_offset", offset + rows.size());
        d.addProperty("truncated", more);
        return d;
    }

    private JsonObject lookup(JsonObject p) {
        if (!p.has("resource") || !p.has("direction")) return BridgeResponses.error("resource and direction required");
        String dir = p.get("direction")
            .getAsString();
        if (!"produces".equals(dir) && !"consumes".equals(dir)) return BridgeResponses.error("invalid direction");
        java.util.List<ItemStack> focus = resolve(p.getAsJsonObject("resource"));
        if (focus.isEmpty()) return BridgeResponses.error("resource not found");
        int offset = integer(p, "offset", 0, 0, 100000), limit = integer(p, "limit", 50, 1, 200), skip = 0;
        java.util.HashSet<String> ids = new java.util.HashSet<String>();
        JsonArray a = new JsonArray();
        boolean more = false;
        for (ItemStack s : focus) {
            java.util.List<? extends IRecipeHandler> hs = "produces".equals(dir)
                ? GuiCraftingRecipe.getCraftingHandlers("item", s.copy())
                : GuiUsageRecipe.getUsageHandlers("item", s.copy());
            for (IRecipeHandler h : hs) for (int i = 0; i < h.numRecipes(); i++) {
                JsonObject r = normalize(h, i, Recipe.of(h, i), false);
                if (r == null || !ids.add(
                    r.get("id")
                        .toString()))
                    continue;
                if (skip++ < offset) continue;
                if (a.size() >= limit) {
                    more = true;
                    break;
                }
                a.add(r);
            }
        }
        JsonObject d = new JsonObject();
        d.add("recipes", a);
        d.addProperty("truncated", more);
        d.addProperty("offset", offset);
        d.addProperty("next_offset", offset + a.size());
        return d;
    }

    private JsonObject graph(JsonObject p) {
        if (!p.has("ends") || !p.get("ends")
            .isJsonArray()) return BridgeResponses.error("ends required");
        int max = integer(p, "max_recipes", 200, 1, 500), depth = integer(p, "max_depth", 4, 1, 16),
            maxResources = integer(p, "max_resources", 300, 1, 1000);
        long deadline = System.nanoTime() + 500000000L;
        java.util.HashSet<String> stops = new java.util.HashSet<String>();
        if (p.has("starts") && p.get("starts")
            .isJsonArray())
            for (com.google.gson.JsonElement e : p.getAsJsonArray("starts"))
                for (ItemStack st : resolveText(e.getAsString())) stops.add(ResourceNormalizer.resourceID(st));
        java.util.ArrayList<ItemStack> q = new java.util.ArrayList<ItemStack>();
        for (com.google.gson.JsonElement e : p.getAsJsonArray("ends")) {
            q.addAll(resolveText(e.getAsString()));
        }
        JsonArray out = new JsonArray();
        java.util.HashSet<String> seen = new java.util.HashSet<String>(), resources = new java.util.HashSet<String>();
        java.util.ArrayList<String> reasons = new java.util.ArrayList<String>();
        for (int d = 0; d < depth && !q.isEmpty() && out.size() < max; d++) {
            java.util.ArrayList<ItemStack> next = new java.util.ArrayList<ItemStack>();
            for (ItemStack s : q) {
                if (System.nanoTime() > deadline) {
                    reasons.add("deadline");
                    break;
                }
                if (!resources.add(ResourceNormalizer.resourceID(s))) continue;
                if (resources.size() > maxResources) {
                    reasons.add("max_resources");
                    break;
                }
                for (IRecipeHandler h : GuiCraftingRecipe.getCraftingHandlers("item", s.copy()))
                    for (int i = 0; i < h.numRecipes() && out.size() < max; i++) {
                        Recipe raw = Recipe.of(h, i);
                        JsonObject r = normalize(h, i, raw, true);
                        if (r == null || !seen.add(
                            r.get("id")
                                .toString()))
                            continue;
                        out.add(r);
                        for (Recipe.RecipeIngredient ingredient : raw.getIngredients()) {
                            if (ingredient.getAmount() <= 0) continue;
                            java.util.List<ItemStack> permutations = ingredient.getPermutations();
                            if (!permutations.isEmpty()
                                && !stops.contains(ResourceNormalizer.resourceID(permutations.get(0))))
                                next.add(
                                    permutations.get(0)
                                        .copy());
                        }
                    }
            }
            q = next;
        }
        if (out.size() >= max) reasons.add("max_recipes");
        else if (!q.isEmpty() && reasons.isEmpty()) reasons.add("max_depth");
        JsonObject data = new JsonObject();
        data.add("recipes", out);
        data.addProperty("truncated", !reasons.isEmpty());
        JsonArray rs = new JsonArray();
        for (String reason : reasons) rs.add(new JsonPrimitive(reason));
        data.add("truncation_reasons", rs);
        return data;
    }

    private java.util.List<ItemStack> resolve(JsonObject s) {
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

    private java.util.List<ItemStack> resolveText(String text) {
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

    private JsonObject recipeGet(JsonObject p) {
        if (!p.has("id") || !p.get("id")
            .isJsonObject()) return BridgeResponses.error("native RecipeId object required");
        JsonObject d = new JsonObject();
        JsonObject r = null;
        try {
            codechicken.nei.recipe.Recipe nativeRecipe = codechicken.nei.recipe.Recipe
                .of(codechicken.nei.recipe.Recipe.RecipeId.of(p.getAsJsonObject("id")));
            if (nativeRecipe != null) r = normalize(null, -1, nativeRecipe, false);
        } catch (Exception ignored) {}
        d.addProperty("found", r != null);
        if (r != null) d.add("recipe", r);
        if (r == null) d.addProperty("error", "RecipeId could not be reconstructed by NEI");
        return d;
    }

    private JsonObject normalize(IRecipeHandler handler, int index, Recipe r, boolean includeResourceIDs) {
        if (r == null || r.getRecipeId() == null) return null;
        JsonObject o = new JsonObject();
        o.add(
            "id",
            r.getRecipeId()
                .toJsonObject());
        o.addProperty(
            "handler_id",
            r.getRecipeId()
                .getHandleName());
        o.addProperty("handler_name", r.getHandlerName());
        o.add("inputs", groups(r.getIngredients(), true, includeResourceIDs));
        o.add("outputs", groups(r.getResults(), false, includeResourceIDs));
        if (handler instanceof gregtech.nei.GTNEIDefaultHandler) {
            gregtech.nei.GTNEIDefaultHandler gt = (gregtech.nei.GTNEIDefaultHandler) handler;
            if (gt.getRecipeMap() != null) o.addProperty(
                "recipe_map",
                gt.getRecipeMap()
                    .toString());
        }
        return o;
    }

    private JsonArray groups(java.util.List<Recipe.RecipeIngredient> in, boolean consumed, boolean includeResourceIDs) {
        JsonArray a = new JsonArray();
        for (Recipe.RecipeIngredient g : in) {
            JsonObject x = new JsonObject();
            JsonArray alts = new JsonArray();
            for (ItemStack s : g.getPermutations()) alts.add(
                includeResourceIDs ? ResourceNormalizer.identifiedResource(s, g.getAmount(), player(), 2)
                    : ResourceNormalizer.resource(s, g.getAmount(), player(), 2));
            x.add("alternatives", alts);
            x.addProperty("consumed", consumed && g.getAmount() > 0);
            x.addProperty("catalyst", consumed && g.getAmount() <= 0);
            x.addProperty("chance", g.getChance() == 0 ? 1.0 : g.getChance() / 10000.0);
            a.add(x);
        }
        return a;
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
