package dev.gtnhmcp.core;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Stable item and fluid identity shared by every tool. */
public interface Items {

    /** A compact, round-trippable id accepted by {@link #resolve}. */
    String id(ItemStack stack);

    /** Resolves an id, or else NEI search text; throws {@link ToolException} if nothing or several items match. */
    ItemStack resolve(String idOrQuery);

    Item item(ItemStack stack);

    /** A fluid amount as recipes show it; throws {@link ToolException} for a fluid NEI cannot show. */
    Stack fluid(FluidStack fluid);

    /** Localized name without formatting codes. */
    String name(ItemStack stack);

    /** Tooltip lines without formatting codes, name excluded. */
    List<String> tooltip(ItemStack stack);

    default Stack stack(ItemStack stack, long amount) {
        return new Stack(item(stack), amount);
    }
}
