package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Stack;

/** Items in any vanilla inventory, as far as the server syncs them to the client (chests often do not). */
final class InventoryDetail implements BlockDetail {

    private final Items items;

    InventoryDetail(Items items) {
        this.items = items;
    }

    public void describe(BlockAt block, Map<String, Object> details) {
        if (!(block.tile() instanceof IInventory inventory)) return;
        List<Stack> stacks = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() != null) stacks.add(items.stack(stack, stack.stackSize));
        }
        details.put("inventory", stacks);
    }
}
