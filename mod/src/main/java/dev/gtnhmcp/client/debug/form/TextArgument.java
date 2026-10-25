package dev.gtnhmcp.client.debug.form;

public final class TextArgument extends TextualArgument {

    public TextArgument(String name, String description, boolean required) {
        super(name, description, required);
    }

    public String hint() {
        return "text";
    }

    protected Object parse(String text) {
        return text;
    }
}
