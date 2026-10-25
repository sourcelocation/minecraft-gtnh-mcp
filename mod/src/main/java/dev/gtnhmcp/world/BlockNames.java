package dev.gtnhmcp.world;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Text;

/** Names blocks for any mod by the item middle-click would give; mods return the specific machine there. */
final class BlockNames {

    private final Items items;
    /** Blocks without a tile entity name the same everywhere, so large walks name each kind once. */
    private final Map<Long, String> plain = new HashMap<>();

    BlockNames(Items items) {
        this.items = items;
    }

    String name(BlockAt at) {
        if (at.tile() != null) return pickName(at);
        return plain.computeIfAbsent(((long) Block.getIdFromBlock(at.block()) << 32) | at.meta(), key -> pickName(at));
    }

    private String pickName(BlockAt at) {
        World world = at.world();
        try {
            MovingObjectPosition target = new MovingObjectPosition(
                at.x(),
                at.y(),
                at.z(),
                1,
                Vec3.createVectorHelper(at.x(), at.y(), at.z()));
            ItemStack picked = at.block()
                .getPickBlock(target, world, at.x(), at.y(), at.z());
            if (picked != null && picked.getItem() != null) return items.name(picked);
        } catch (RuntimeException ignored) {
            // Some blocks cannot be picked outside normal play; their block name will do.
        }
        return Text.clean(
            at.block()
                .getLocalizedName());
    }
}
