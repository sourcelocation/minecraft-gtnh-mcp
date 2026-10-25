package dev.gtnhmcp.output;

import dev.gtnhmcp.core.Stack;

/** {@code 24× Torch [minecraft:torch]}, {@code 144 mB Water [fluid:water]}; a single item has no count. */
final class StackLabel extends Label<Stack> {

    private final ItemLabel item;

    StackLabel(ItemLabel item) {
        super(Stack.class);
        this.item = item;
    }

    public String describe() {
        return "'<n>× <name> [<id>]', n omitted when 1; fluids '<n> mB <name> [<id>]'";
    }

    public String render(Stack stack) {
        return stack.item()
            .unit()
            .count(stack.amount()) + item.render(stack.item());
    }
}
