package dev.gtnhmcp.client.debug.form;

/** An argument typed as text and parsed into its value; one text field can edit every subclass. */
public abstract class TextualArgument extends Argument {

    private String text = "";

    protected TextualArgument(String name, String description, boolean required) {
        super(name, description, required);
    }

    public final String text() {
        return text;
    }

    public final void text(String text) {
        this.text = text;
    }

    /** A short hint shown in the empty field, e.g. the expected format. */
    public abstract String hint();

    public final Object value() {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : parse(trimmed);
    }

    /** Parses non-blank, trimmed text. */
    protected abstract Object parse(String text);
}
