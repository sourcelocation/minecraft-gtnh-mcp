package dev.gtnhmcp.core;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** An amount of an item, or of a fluid in mB when the id starts with {@code fluid:}. */
public record Stack(String id, String name, @JsonPropertyDescription("items, or mB for fluids") long amount) {}
