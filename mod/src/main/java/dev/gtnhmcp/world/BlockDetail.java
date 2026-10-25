package dev.gtnhmcp.world;

import java.util.Map;

/** Adds one kind of detail about a listed block, through an interface every mod implements; empty adds nothing. */
interface BlockDetail {

    void describe(BlockAt block, Map<String, Object> details);
}
