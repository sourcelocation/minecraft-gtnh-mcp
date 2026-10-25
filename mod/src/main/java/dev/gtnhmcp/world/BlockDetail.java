package dev.gtnhmcp.world;

import java.util.Map;

/** Adds one kind of detail about a listed block, through an interface every mod implements; none adds nothing. */
interface BlockDetail {

    /** For contents that read as empty: the client cannot tell empty from not sent (machines often send nothing). */
    String NONE_VISIBLE = "none visible";

    void describe(BlockAt block, Map<String, Object> details);
}
