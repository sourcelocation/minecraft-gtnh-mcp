package dev.gtnhmcp.item;

import static dev.gtnhmcp.core.Text.clean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import codechicken.nei.ItemList;
import codechicken.nei.SearchField;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.recipe.StackInfo;
import dev.gtnhmcp.core.Item;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.ToolException;

/**
 * Ids are NEI's own stack NBT written in the first {@link IdFormat} that applies. When the NBT is longer than
 * {@link #MAX_ID}
 * (tool stats, toolbox contents, UUIDs), its tag is left out of the id; get_slot still shows them. Names are the game's
 * own: NEI's fluid
 * display stacks already name themselves after their fluid.
 */
public final class NeiItems implements Items {

    private static final int MAX_ID = 96;

    private final List<IdFormat> formats = List.of(new FluidId(), new RegistryId(), new NbtId());

    public String id(ItemStack stack) {
        NBTTagCompound nbt = nbt(stack);
        return format(nbt).encode(nbt);
    }

    public Item item(ItemStack stack) {
        NBTTagCompound nbt = nbt(stack);
        IdFormat format = format(nbt);
        return new Item(format.encode(nbt), name(stack), format.unit());
    }

    public ItemStack resolve(String idOrQuery) {
        for (IdFormat format : formats) {
            NBTTagCompound nbt = format.decode(idOrQuery);
            ItemStack stack = nbt == null ? null : load(nbt);
            if (stack != null) return stack;
        }
        return search(idOrQuery);
    }

    public String name(ItemStack stack) {
        return clean(stack.getDisplayName());
    }

    public List<String> tooltip(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<String> raw = stack.getTooltip(Minecraft.getMinecraft().thePlayer, false);
            for (String line : raw.subList(Math.min(1, raw.size()), raw.size())) {
                String text = clean(line);
                if (!text.isEmpty()) lines.add(text);
            }
        } catch (RuntimeException ignored) {
            // Some mods' tooltips assume a GUI context; a missing tooltip is not an error.
        }
        return lines;
    }

    /** NEI's NBT for the stack, without its count, and without its tag when that would make a long id. */
    private static NBTTagCompound nbt(ItemStack stack) {
        NBTTagCompound nbt = StackInfo.itemStackToNBT(stack, false);
        if (nbt == null) throw new ToolException("Item has no registry name");
        nbt.removeTag("Count");
        if (nbt.hasKey("tag") && nbt.toString()
            .length() > MAX_ID) nbt.removeTag("tag");
        return nbt;
    }

    /** The last format accepts any NBT, so there is always one. */
    private IdFormat format(NBTTagCompound nbt) {
        for (IdFormat format : formats) if (format.encode(nbt) != null) return format;
        throw new IllegalStateException("No id format for " + nbt);
    }

    private static ItemStack load(NBTTagCompound nbt) {
        nbt.setInteger("Count", 1);
        try {
            return StackInfo.loadFromNBT(nbt);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private ItemStack search(String query) {
        if (!ItemList.loadFinished) throw new ToolException("NEI is still loading items, retry shortly");
        ItemFilter filter = SearchField.getFilter(query);
        List<ItemStack> hits = new ArrayList<>();
        for (ItemStack stack : ItemList.items) {
            if (!filter.matches(stack)) continue;
            if (name(stack).equalsIgnoreCase(query)) return stack;
            if (hits.size() < 5) hits.add(stack);
        }
        if (hits.size() == 1) return hits.get(0);
        if (hits.isEmpty()) throw new ToolException("No item matches '" + query + "'");
        throw new ToolException(
            "'" + query
                + "' is ambiguous; use an id: "
                + hits.stream()
                    .map(s -> id(s) + " (" + name(s) + ")")
                    .collect(Collectors.joining(", ")));
    }
}
