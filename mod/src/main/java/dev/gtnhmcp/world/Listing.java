package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Every block whose name matches, one entry each with its exact position and details, up to a limit. */
final class Listing implements BlockSink {

    public record Entry(@JsonPropertyDescription("x,y,z") String at, String name,
        @JsonPropertyDescription("e.g. inventory and fluids, as far as the client can see them") Map<String, Object> details) {}

    /** Matches every block. */
    static final String ALL = "*";

    private final String filter;
    private final int limit;
    private final List<BlockDetail> details;
    private final List<Entry> entries = new ArrayList<>();
    private int unlisted;

    /** @param filter text in the block name, case-insensitive, or {@link #ALL} */
    Listing(String filter, int limit, List<BlockDetail> details) {
        this.filter = filter.toLowerCase(Locale.ROOT);
        this.limit = limit;
        this.details = List.copyOf(details);
    }

    public void accept(BlockAt block, String name) {
        if (!matches(name)) return;
        if (entries.size() >= limit) {
            unlisted++;
            return;
        }
        Map<String, Object> facts = new LinkedHashMap<>();
        for (BlockDetail detail : details) detail.describe(block, facts);
        entries.add(new Entry(block.at(), name, facts));
    }

    List<Entry> entries() {
        return entries;
    }

    /** Matching blocks past the limit. */
    int unlisted() {
        return unlisted;
    }

    private boolean matches(String name) {
        return filter.equals(ALL) || name.toLowerCase(Locale.ROOT)
            .contains(filter);
    }
}
