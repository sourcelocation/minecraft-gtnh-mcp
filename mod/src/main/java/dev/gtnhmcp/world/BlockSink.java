package dev.gtnhmcp.world;

/** Receives every block of a walk with its name; each sink builds one view of the area. */
interface BlockSink {

    void accept(BlockAt block, String name);
}
