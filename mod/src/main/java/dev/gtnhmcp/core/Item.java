package dev.gtnhmcp.core;

/** An item or fluid: an id accepted by every tool, and its display name. */
public record Item(String id, String name) {

    /** Fluid ids start with this; their amounts are mB. */
    public static final String FLUID = "fluid:";

    public boolean fluid() {
        return id.startsWith(FLUID);
    }
}
