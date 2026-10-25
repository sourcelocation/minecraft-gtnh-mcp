package dev.gtnhmcp.client.debug.form;

/** One field of a tool's input record, edited in the debug screen and turned into the value a client would send. */
public abstract class Argument {

    private final String name;
    private final String description;
    private final boolean required;

    protected Argument(String name, String description, boolean required) {
        this.name = name;
        this.description = description;
        this.required = required;
    }

    public final String name() {
        return name;
    }

    public final String description() {
        return description;
    }

    public final boolean required() {
        return required;
    }

    /** The JSON-ready value, or null when left empty; throws {@link InvalidArgument} for unusable input. */
    public abstract Object value();
}
