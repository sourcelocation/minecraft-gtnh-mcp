package dev.gtnhmcp.output;

import dev.gtnhmcp.core.Item;

/** {@code Torch [minecraft:torch]}: the name first, the id last so names never make it ambiguous. */
final class ItemLabel extends Label<Item> {

    ItemLabel() {
        super(Item.class);
    }

    public String describe() {
        return "'<name> [<id>]'";
    }

    public String render(Item item) {
        return item.name() + " [" + item.id() + "]";
    }
}
