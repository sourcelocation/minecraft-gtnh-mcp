package dev.gtnhmcp.client.debug.form;

/** An optional boolean: unset, true or false. */
public final class FlagArgument extends Argument {

    private Boolean state;

    public FlagArgument(String name, String description, boolean required) {
        super(name, description, required);
    }

    /** Unset, then true, then false, then unset again. */
    public void cycle() {
        state = state == null ? Boolean.TRUE : state ? Boolean.FALSE : null;
    }

    public String label() {
        return state == null ? "unset" : state.toString();
    }

    public Object value() {
        return state;
    }
}
