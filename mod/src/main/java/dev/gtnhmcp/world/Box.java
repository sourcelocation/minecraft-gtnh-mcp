package dev.gtnhmcp.world;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import dev.gtnhmcp.core.ToolException;

/** An inclusive block box in one dimension. */
record Box(int dim, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    private static final int WORLD_TOP = 255;

    /** Corners when both are given, else a cube of {@code radius} (or the default) around the player. */
    static Box of(EntityPlayer player, Integer radius, List<Integer> from, List<Integer> to, int defaultRadius) {
        if (from != null || to != null) return between(player.dimension, from, to);
        int r = radius == null ? defaultRadius : radius;
        if (r < 0) throw new ToolException("radius must not be negative");
        int x = MathHelper.floor_double(player.posX), y = MathHelper.floor_double(player.posY),
            z = MathHelper.floor_double(player.posZ);
        return new Box(player.dimension, x - r, Math.max(0, y - r), z - r, x + r, Math.min(WORLD_TOP, y + r), z + r);
    }

    static Box between(int dim, List<Integer> from, List<Integer> to) {
        if (from == null || to == null || from.size() != 3 || to.size() != 3)
            throw new ToolException("from and to are both [x, y, z]");
        return new Box(
            dim,
            Math.min(from.get(0), to.get(0)),
            Math.max(0, Math.min(from.get(1), to.get(1))),
            Math.min(from.get(2), to.get(2)),
            Math.max(from.get(0), to.get(0)),
            Math.min(WORLD_TOP, Math.max(from.get(1), to.get(1))),
            Math.max(from.get(2), to.get(2)));
    }

    /** Rejects boxes wider than {@code maxSide} on any axis, which would be slow to scan and long to read. */
    Box limit(int maxSide) {
        if (maxX - minX >= maxSide || maxY - minY >= maxSide || maxZ - minZ >= maxSide)
            throw new ToolException("Area too large; at most " + maxSide + " blocks per side");
        return this;
    }

    boolean intersects(Box other) {
        return dim == other.dim && minX <= other.maxX
            && maxX >= other.minX
            && minY <= other.maxY
            && maxY >= other.minY
            && minZ <= other.maxZ
            && maxZ >= other.minZ;
    }

    /** Horizontal distance from the player to the box's centre, or null in another dimension. */
    Integer distanceTo(EntityPlayer player) {
        if (player.dimension != dim) return null;
        return (int) Math.hypot((minX + maxX) / 2.0 - player.posX, (minZ + maxZ) / 2.0 - player.posZ);
    }

    /** {@code x1,y1,z1..x2,y2,z2}. */
    String text() {
        return minX + "," + minY + "," + minZ + ".." + maxX + "," + maxY + "," + maxZ;
    }
}
