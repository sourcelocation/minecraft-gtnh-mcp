package dev.gtnhmcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

final class BridgeResponses {

    private BridgeResponses() {}

    static JsonObject response(JsonObject data, String source, String completeness) {
        return response(data, source, completeness, null);
    }

    static JsonObject response(JsonObject data, String source, String completeness, String warning) {
        JsonObject result = new JsonObject();
        JsonObject envelope = new JsonObject();
        envelope.addProperty("schema", "gtnh-mcp/v1");
        envelope.addProperty("source", source);
        envelope.addProperty(
            "captured_at",
            java.time.Instant.now()
                .toString());
        envelope.addProperty("completeness", completeness);
        JsonArray warnings = new JsonArray();
        if (warning != null) warnings.add(new JsonPrimitive(warning));
        envelope.add("warnings", warnings);
        result.add("envelope", envelope);
        result.add("data", data);
        return result;
    }

    static JsonObject unsupported(String reason) {
        return response(new JsonObject(), "client", "unsupported", reason);
    }
}
