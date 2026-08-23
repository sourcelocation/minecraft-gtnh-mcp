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
        register("catalog_search", recipes);
        register("recipe_lookup", recipes);
        register("recipes.graph", recipes);
        register("recipe_get", recipes);
        if (Loader.isModLoaded("betterquesting")) register("quests_query", new QuestProvider());
        StatusProvider status = new StatusProvider(providers.keySet());
        register("status", status);
        register("player", status);
    }

    private void register(String method, QueryProvider provider) {
        providers.put(method, provider);
    }

    public JsonObject query(final String method, final JsonObject params) throws Exception {
        final QueryProvider provider = providers.get(method);
        if (provider == null) return BridgeResponses.unsupported("unknown method: " + method);
        return dispatcher.call(new Callable<JsonObject>() {

            public JsonObject call() {
                return provider.query(method, params);
            }
        }, 1500);
    }
}
