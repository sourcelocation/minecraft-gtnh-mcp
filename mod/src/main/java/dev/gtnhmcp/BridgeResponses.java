package dev.gtnhmcp;

import com.google.gson.JsonObject;

final class BridgeResponses {

    private BridgeResponses() {}

    static JsonObject error(String reason) {
        JsonObject result = new JsonObject();
        result.addProperty("error", reason);
        return result;
    }
}
