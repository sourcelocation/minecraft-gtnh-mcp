package dev.gtnhmcp.output;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/** Writes one value type as a single string, the way the game shows it, instead of a JSON object. */
public abstract class Label<T> extends ValueSerializer<T> {

    private final Class<T> type;

    protected Label(Class<T> type) {
        this.type = type;
    }

    public final Class<T> handledType() {
        return type;
    }

    /** How to read the string; it becomes the type's schema description. */
    public abstract String describe();

    public abstract String render(T value);

    public final void serialize(T value, JsonGenerator generator, SerializationContext context) {
        generator.writeString(render(value));
    }
}
