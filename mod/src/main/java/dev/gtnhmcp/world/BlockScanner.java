package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Reads a box of loaded blocks into groups by name; few-block groups keep their positions, big ones a bounding box. */
final class BlockScanner {

    /** {@code at} lists every position of small groups; larger groups give {@code bounds} instead. */
    public record Group(int count, @JsonPropertyDescription("x,y,z of each block") List<String> at,
        @JsonPropertyDescription("x1,y1,z1..x2,y2,z2 around all of them") String bounds) {}

    /** Groups ordered rarest first, so machines come before casings and stone. */
    public record Scan(Map<String, Group> blocks,
        @JsonPropertyDescription("true when part of the area is in unloaded chunks and was skipped") boolean partial) {}

    private final BlockNames names;

    BlockScanner(BlockNames names) {
        this.names = names;
    }

    Scan scan(World world, Box box, Predicate<String> keep, int listUpTo) {
        Map<String, Collector> groups = new HashMap<>();
        boolean partial = false;
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            if (!world.blockExists(x, 0, z)) {
                partial = true;
                continue;
            }
            for (int y = box.minY(); y <= box.maxY(); y++) {
                Block block = world.getBlock(x, y, z);
                if (block.isAir(world, x, y, z)) continue;
                String name = names.name(world, x, y, z, block);
                if (keep.test(name)) groups.computeIfAbsent(name, key -> new Collector(listUpTo))
                    .add(x, y, z);
            }
        }
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
        return new Scan(ordered, partial);
    }

    private static final class Collector {

        private final int listUpTo;
        private final List<String> at = new ArrayList<>();
        private int count;
        private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        Collector(int listUpTo) {
            this.listUpTo = listUpTo;
        }

        void add(int x, int y, int z) {
            count++;
            if (at.size() < listUpTo) at.add(x + "," + y + "," + z);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        Group group() {
            if (count <= listUpTo) return new Group(count, at, null);
            return new Group(count, null, new Box(0, minX, minY, minZ, maxX, maxY, maxZ).text());
        }
    }
}
