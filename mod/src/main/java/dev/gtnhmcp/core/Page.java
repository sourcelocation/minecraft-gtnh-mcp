package dev.gtnhmcp.core;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** One page of results. */
public record Page<T> (List<T> items, @JsonPropertyDescription("pass as cursor for the next page") String next) {

    /** Slices a lazy stream at an opaque cursor, reading one extra element to know whether more exist. */
    public static <T> Page<T> of(Stream<T> all, String cursor, Integer limit) {
        int size = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        int offset = parse(cursor);
        List<T> items = all.skip(offset)
            .limit(size + 1L)
            .toList();
        return items.size() > size ? new Page<>(items.subList(0, size), Integer.toString(offset + size))
            : new Page<>(items, null);
    }

    private static int parse(String cursor) {
        if (cursor == null || cursor.isEmpty()) return 0;
        try {
            return Math.max(0, Integer.parseInt(cursor));
        } catch (NumberFormatException e) {
            throw new ToolException("Invalid cursor");
        }
    }
}
