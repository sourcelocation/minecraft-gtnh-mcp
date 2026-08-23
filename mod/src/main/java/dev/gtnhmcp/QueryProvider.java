package dev.gtnhmcp;

import com.google.gson.JsonObject;

interface QueryProvider {

    JsonObject query(String method, JsonObject params);
}
