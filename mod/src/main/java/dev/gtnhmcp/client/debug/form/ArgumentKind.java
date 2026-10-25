package dev.gtnhmcp.client.debug.form;

import java.lang.reflect.Type;

/** Builds the {@link Argument} for one record component type; add a kind to support a new type. */
public interface ArgumentKind {

    /** What the form knows about a record component, from its type and Jackson annotations. */
    record Field(String name, String description, boolean required, Type type) {}

    boolean accepts(Type type);

    Argument create(Field field, ArgumentForms forms);
}
