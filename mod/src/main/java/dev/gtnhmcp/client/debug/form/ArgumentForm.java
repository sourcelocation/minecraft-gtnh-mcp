package dev.gtnhmcp.client.debug.form;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The arguments of one record, e.g. a tool's input; produces the map an MCP client would send. */
public final class ArgumentForm {

    private final List<Argument> arguments;

    ArgumentForm(List<Argument> arguments) {
        this.arguments = List.copyOf(arguments);
    }

    public List<Argument> arguments() {
        return arguments;
    }

    public boolean isEmpty() {
        return arguments.stream()
            .allMatch(argument -> argument.value() == null);
    }

    /**
     * Values of the filled-in arguments; throws {@link InvalidArgument} when one is invalid or a required one is empty.
     */
    public Map<String, Object> values() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Argument argument : arguments) {
            Object value = argument.value();
            if (value != null) values.put(argument.name(), value);
            else if (argument.required()) throw new InvalidArgument(argument.name() + " is required");
        }
        return values;
    }
}
