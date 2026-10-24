package dev.gtnhmcp.core;

import java.lang.reflect.Type;

/**
 * One read-only MCP tool. Input and output are plain records; their JSON Schemas are generated, so the Java types are
 * the only definition of the contract. Calls run on the client thread.
 */
public interface Tool<I, O> {

    String name();

    String description();

    Type input();

    Type output();

    O call(I input);
}
