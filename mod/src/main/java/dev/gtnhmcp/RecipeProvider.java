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

    public JsonObject query(String method, JsonObject params) {
        if ("item_lookup".equals(method)) return itemLookup(params);
        if ("lookup_recipes".equals(method)) return lookup(params, true);
        if ("lookup_uses".equals(method)) return lookup(params, false);
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
        if (!p.has("resource")) return BridgeResponses.error("resource required");
        java.util.List<ItemStack> focus = resolve(p.getAsJsonObject("resource"));
        if (focus.isEmpty()) return BridgeResponses.error("resource not found");
        int offset = integer(p, "offset", 0, 0, 100000), limit = integer(p, "limit", 50, 1, 200), skip = 0;
        java.util.HashSet<String> ids = new java.util.HashSet<String>();
        JsonArray a = new JsonArray();
        boolean more = false;
        for (ItemStack s : focus) {
            java.util.List<? extends IRecipeHandler> hs = recipes
                ? GuiCraftingRecipe.getCraftingHandlers("item", s.copy())
                : GuiUsageRecipe.getUsageHandlers("item", s.copy());
            for (IRecipeHandler h : hs) for (int i = 0; i < h.numRecipes(); i++) {
                JsonObject r = normalize(h, i, Recipe.of(h, i));
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

    private JsonObject recipeGet(JsonObject p) {
        if (!p.has("id") || !p.get("id")
            .isJsonObject()) return BridgeResponses.error("native RecipeId object required");
        JsonObject d = new JsonObject();
        JsonObject r = null;
        try {
            codechicken.nei.recipe.Recipe nativeRecipe = codechicken.nei.recipe.Recipe
                .of(codechicken.nei.recipe.Recipe.RecipeId.of(p.getAsJsonObject("id")));
            if (nativeRecipe != null) r = normalize(null, -1, nativeRecipe);
        } catch (Exception ignored) {}
        d.addProperty("found", r != null);
        if (r != null) d.add("recipe", r);
        if (r == null) d.addProperty("error", "RecipeId could not be reconstructed by NEI");
        return d;
    }

    private JsonObject normalize(IRecipeHandler handler, int index, Recipe r) {
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
        o.add("inputs", groups(r.getIngredients(), true));
        o.add("outputs", groups(r.getResults(), false));
        if (handler instanceof gregtech.nei.GTNEIDefaultHandler) {
            gregtech.nei.GTNEIDefaultHandler gt = (gregtech.nei.GTNEIDefaultHandler) handler;
            if (gt.getRecipeMap() != null) o.addProperty(
                "recipe_map",
                gt.getRecipeMap()
                    .toString());
        }
        return o;
    }

    JsonArray groups(java.util.List<Recipe.RecipeIngredient> in, boolean consumed) {
        JsonArray a = new JsonArray();
        for (Recipe.RecipeIngredient g : in) {
            JsonObject x = new JsonObject();
            JsonArray alts = new JsonArray();
            for (ItemStack s : g.getPermutations())
                alts.add(ResourceNormalizer.resource(s, g.getAmount(), player(), 2));
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
