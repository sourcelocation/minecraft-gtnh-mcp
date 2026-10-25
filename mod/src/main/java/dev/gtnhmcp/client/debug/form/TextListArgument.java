package dev.gtnhmcp.client.debug.form;

import java.util.Arrays;

/** A list of strings typed comma-separated, e.g. item ids. */
public final class TextListArgument extends TextualArgument {

    public TextListArgument(String name, String description, boolean required) {
        super(name, description, required);
    }

    public String hint() {
        return "comma-separated";
    }

    protected Object parse(String text) {
        return Arrays.stream(text.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
