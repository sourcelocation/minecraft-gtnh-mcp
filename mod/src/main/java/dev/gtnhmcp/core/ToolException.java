package dev.gtnhmcp.core;

/** A failure the model can act on; its message is returned as the tool error. */
public final class ToolException extends RuntimeException {

    public ToolException(String message) {
        super(message);
    }
}
