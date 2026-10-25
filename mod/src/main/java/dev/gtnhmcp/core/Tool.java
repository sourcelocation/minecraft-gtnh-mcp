package dev.gtnhmcp.core;

import java.lang.reflect.Type;

/**
 * One MCP tool. Input and output are plain records; their JSON Schemas are generated, so the Java types are the only
 * definition of the contract. Calls run on the client thread. No tool ever changes the game.
 */
public interface Tool<I, O> {

    /** What a call may change: nothing, or this mod's own notes about the world (e.g. region labels). */
    enum Access {
        READ,
        WRITE,
        DELETE
    }

    String name();

    default Access access() {
        return Access.READ;
    }

    /** Short display name for client UIs and the debug screen; not sent to the model. */
    String title();

    String description();

    Type input();

    Type output();

    O call(I input);
}
