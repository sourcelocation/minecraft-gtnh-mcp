package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Text;

/**
 * Names blocks for any mod the way the player sees them: the item middle-click would give (mods return the specific
 * machine there), plus what an inventory shows the client. Client-side inventories are only what the server syncs, so
 * chests often read empty while drawers and machines with displays do not.
 */
final class BlockNames {

    private static final int SHOWN = 4;

    private final Items items;
    /** Plain blocks name the same everywhere, so large scans name each kind once. */
    private final Map<Long, String> plain = new HashMap<>();

    BlockNames(Items items) {
        this.items = items;
    }

    String name(World world, int x, int y, int z, Block block) {
        int meta = world.getBlockMetadata(x, y, z);
        if (!block.hasTileEntity(meta)) return plain
            .computeIfAbsent(((long) Block.getIdFromBlock(block) << 32) | meta, key -> pickName(world, x, y, z, block));
        String name = pickName(world, x, y, z, block);
        String contents = world.getTileEntity(x, y, z) instanceof IInventory inventory ? contents(inventory) : "";
        return contents.isEmpty() ? name : name + ": " + contents;
    }

    private String pickName(World world, int x, int y, int z, Block block) {
        try {
            MovingObjectPosition target = new MovingObjectPosition(x, y, z, 1, Vec3.createVectorHelper(x, y, z));
            ItemStack picked = block.getPickBlock(target, world, x, y, z);
            if (picked != null && picked.getItem() != null) return items.name(picked);
        } catch (RuntimeException ignored) {
            // Some blocks cannot be picked outside normal play; their block name will do.
        }
        return Text.clean(block.getLocalizedName());
    }

    private String contents(IInventory inventory) {
        List<String> stacks = new ArrayList<>();
        int more = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null || stack.getItem() == null) continue;
            if (stacks.size() < SHOWN) stacks.add(stack.stackSize + "× " + items.name(stack));
            else more++;
        }
        if (more > 0) stacks.add("+" + more + " more");
        return String.join(", ", stacks);
    }
}
