package dev.gtnhmcp.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Text;

/** Names blocks: tile entities through the describers, everything else by its item name, cached per block and meta. */
final class BlockNames {

    private final List<BlockDescriber> describers;
    private final Items items;
    private final Map<Long, String> plain = new HashMap<>();

    BlockNames(List<BlockDescriber> describers, Items items) {
        this.describers = List.copyOf(describers);
        this.items = items;
    }

    String name(World world, int x, int y, int z, Block block) {
        int meta = world.getBlockMetadata(x, y, z);
        if (block.hasTileEntity(meta)) {
            TileEntity tile = world.getTileEntity(x, y, z);
            for (BlockDescriber describer : describers) {
                String name = tile == null ? null : describer.describe(tile, items);
                if (name != null) return name;
            }
        }
        return plain.computeIfAbsent(((long) Block.getIdFromBlock(block) << 32) | meta, key -> plainName(block, meta));
    }

    private String plainName(Block block, int meta) {
        if (Item.getItemFromBlock(block) != null) {
            try {
                return items.name(new ItemStack(block, 1, block.damageDropped(meta)));
            } catch (RuntimeException ignored) {
                // Some blocks' items cannot be named outside their usual context; fall back to the block's name.
            }
        }
        return Text.clean(block.getLocalizedName());
    }
}
