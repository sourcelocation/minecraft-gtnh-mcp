package dev.gtnhmcp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.gson.JsonObject;

import cpw.mods.fml.common.Loader;

/** Routes small named queries onto Minecraft's client thread. */
public final class QueryRouter {

    private final ClientDispatcher dispatcher;
    private final Map<String, QueryProvider> providers = new LinkedHashMap<String, QueryProvider>();

    public QueryRouter(ClientDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        RecipeProvider recipes = new RecipeProvider();
        register("item_lookup", recipes);
        register("lookup_recipes", recipes);
        register("lookup_uses", recipes);
        register("recipe_get", recipes);
        register("recipe_paths", new RecipePathProvider(recipes));
        if (Loader.isModLoaded("betterquesting")) register("quests_query", new QuestProvider());
        StatusProvider status = new StatusProvider(providers.keySet());
        register("status", status);
        register("player", status);
        register("player_slot", status);
    }

    private void register(String method, QueryProvider provider) {
        providers.put(method, provider);
    }

    public JsonObject query(final String method, final JsonObject params) throws Exception {
        final QueryProvider provider = providers.get(method);
        if (provider == null) return BridgeResponses.error("unknown method: " + method);
        return dispatcher.call(new Callable<JsonObject>() {

            public JsonObject call() {
                return provider.query(method, params);
            }
        }, 1500);
    }
}
