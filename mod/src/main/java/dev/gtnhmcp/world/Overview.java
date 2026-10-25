package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Blocks grouped by name: few-block groups keep their positions, large ones give a bounding box. */
final class Overview implements BlockSink {

    public record Group(int count, @JsonPropertyDescription("x,y,z of each block") List<String> at,
        @JsonPropertyDescription("x1,y1,z1..x2,y2,z2 around all of them") String bounds) {}

    private final int listUpTo;
    private final Map<String, Collector> groups = new HashMap<>();

    Overview(int listUpTo) {
        this.listUpTo = listUpTo;
    }

    public void accept(BlockAt block, String name) {
        groups.computeIfAbsent(name, key -> new Collector())
            .add(block);
    }

    /** Rarest first, so machines come before casings and stone. */
    Map<String, Group> groups() {
        Map<String, Group> ordered = new LinkedHashMap<>();
        groups.entrySet()
            .stream()
            .sorted(
                Comparator.comparingInt((Map.Entry<String, Collector> e) -> e.getValue().count)
                    .thenComparing(Map.Entry::getKey))
            .forEach(
                e -> ordered.put(
                    e.getKey(),
                    e.getValue()
                        .group()));
        return ordered;
    }

    private final class Collector {

        private final List<String> at = new ArrayList<>();
        private int count;
        private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        void add(BlockAt block) {
            count++;
            if (at.size() < listUpTo) at.add(block.at());
            minX = Math.min(minX, block.x());
            minY = Math.min(minY, block.y());
            minZ = Math.min(minZ, block.z());
            maxX = Math.max(maxX, block.x());
            maxY = Math.max(maxY, block.y());
            maxZ = Math.max(maxZ, block.z());
        }

        Group group() {
            if (count <= listUpTo) return new Group(count, at, null);
            return new Group(count, null, new Box(0, minX, minY, minZ, maxX, maxY, maxZ).text());
        }
    }
}
