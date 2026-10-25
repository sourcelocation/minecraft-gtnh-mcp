package dev.gtnhmcp.server;

import java.util.Map;
import java.util.function.Function;

/** The outcome of one tool call, as MCP clients and the debug screen both see it. */
public sealed interface ToolResult {

    /** Handles both outcomes; callers never test or cast the type. */
    <T> T fold(Function<Success, T> success, Function<Failure, T> failure);

    /** {@code json} is the compact text the model receives; {@code pretty} is the same, indented for people. */
    record Success(Map<String, Object> structured, String json, String pretty) implements ToolResult {

        public <T> T fold(Function<Success, T> success, Function<Failure, T> failure) {
            return success.apply(this);
        }
    }

    record Failure(String message) implements ToolResult {

        public <T> T fold(Function<Success, T> success, Function<Failure, T> failure) {
            return failure.apply(this);
        }
    }
}
