package dev.gtnhmcp.client.debug.form;

import java.util.Arrays;

/** Whole numbers typed comma-separated, e.g. a position {@code -590, 48, -2109}. */
public final class NumberListArgument extends TextualArgument {

    public NumberListArgument(String name, String description, boolean required) {
        super(name, description, required);
    }

    public String hint() {
        return "comma-separated numbers";
    }

    protected Object parse(String text) {
        try {
            return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
        } catch (NumberFormatException e) {
            throw new InvalidArgument(name() + " must be whole numbers separated by commas");
        }
    }
}
