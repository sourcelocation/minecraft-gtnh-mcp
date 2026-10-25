package dev.gtnhmcp.core;

/** An item or fluid: an id accepted by every tool, its display name, and how amounts of it are counted. */
public record Item(String id, String name, Unit unit) {}
