package dev.gtnhmcp.core;

/** An amount of an item, or of a fluid in mB. */
public record Stack(Item item, long amount) {}
