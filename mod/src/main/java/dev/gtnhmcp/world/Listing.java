package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.ToolException;

/** Every block whose name matches, one entry each with its exact position and details, up to a limit. */
final class Listing implements BlockSink {

    public record Entry(@JsonPropertyDescription("x,y,z") String at, String name,
        @JsonPropertyDescription("inventory, fluids and neighbours as far as the client sees them; 'none visible' "
            + "is empty or not sent to the client") Map<String, Object> details) {

        static Entry of(BlockAt block, String name, List<BlockDetail> details) {
            Map<String, Object> facts = new LinkedHashMap<>();
            for (BlockDetail detail : details) detail.describe(block, facts);
            return new Entry(block.at(), name, facts);
        }
    }

    private final String filter;
    private final int limit;
    private final List<BlockDetail> details;
    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Integer> unlisted = new LinkedHashMap<>();

    /** @param filter text in the block name, case-insensitive */
    Listing(String filter, int limit, List<BlockDetail> details) {
        if (filter.isBlank()) throw new ToolException("Name the blocks to list, e.g. from get_blocks' overview");
        this.filter = filter.toLowerCase(Locale.ROOT);
        this.limit = limit;
        this.details = List.copyOf(details);
    }

    public void accept(BlockAt block, String name) {
        if (!name.toLowerCase(Locale.ROOT)
            .contains(filter)) return;
        if (entries.size() >= limit) unlisted.merge(name, 1, Integer::sum);
        else entries.add(Entry.of(block, name, details));
    }

    List<Entry> entries() {
        return entries;
    }

    /** Matching blocks past the limit, counted by name. */
    Map<String, Integer> unlisted() {
        return unlisted;
    }
}
