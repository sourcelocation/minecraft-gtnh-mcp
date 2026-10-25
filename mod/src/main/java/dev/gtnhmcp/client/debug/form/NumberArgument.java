package dev.gtnhmcp.client.debug.form;

public final class NumberArgument extends TextualArgument {

    public NumberArgument(String name, String description, boolean required) {
        super(name, description, required);
    }

    public String hint() {
        return "whole number";
    }

    protected Object parse(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new InvalidArgument(name() + " must be a whole number");
        }
    }
}
