package dev.gtnhmcp.core;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/** Reads the input and output types from the subclass's type arguments, generics included. */
public abstract class AbstractTool<I, O> implements Tool<I, O> {

    private final String name;
    private final String description;
    private final Type input;
    private final Type output;

    protected AbstractTool(String name, String description) {
        Type[] types = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
        this.name = name;
        this.description = description;
        this.input = types[0];
        this.output = types[1];
    }

    public final String name() {
        return name;
    }

    public final String description() {
        return description;
    }

    public final Type input() {
        return input;
    }

    public final Type output() {
        return output;
    }
}
