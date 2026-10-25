package dev.gtnhmcp.core;

import java.util.List;

import net.minecraft.item.ItemStack;

/** Stable item and fluid identity shared by every tool. */
public interface Items {

    /** A compact, round-trippable id: {@code mod:name[:meta]}, {@code fluid:name}, or an NBT string. */
    String id(ItemStack stack);

    /** Resolves an id, or else NEI search text; throws {@link ToolException} if nothing or several items match. */
    ItemStack resolve(String idOrQuery);

    /** Localized name without formatting codes. */
    String name(ItemStack stack);

    /** Tooltip lines without formatting codes, name excluded. */
    List<String> tooltip(ItemStack stack);

    default Item item(ItemStack stack) {
        return new Item(id(stack), name(stack));
    }

    default Stack stack(ItemStack stack, long amount) {
        return new Stack(item(stack), amount);
    }
}
