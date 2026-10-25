package dev.gtnhmcp.item;

import static dev.gtnhmcp.core.Text.clean;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import codechicken.nei.ItemList;
import codechicken.nei.SearchField;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.recipe.StackInfo;
import dev.gtnhmcp.core.Item;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.ToolException;

/**
 * Ids are NEI's own stack NBT (which already maps fluid displays to fluids), shortened for the common cases. NBT longer
 * than {@link #MAX_NBT} (tool stats, toolbox contents) is dropped from the id; get_slot still shows it.
 */
public final class NeiItems implements Items {

    private static final String FLUID = Item.FLUID;
    /** NEI's own fluid key, then GregTech's handler key used by older NEI builds. */
    private static final List<String> FLUID_KEYS = List.of("neiFluidName", "gtFluidName");
    private static final Set<String> PLAIN = Set.of("strId", "Damage");
    private static final int MAX_NBT = 96;

    public String id(ItemStack stack) {
        NBTTagCompound nbt = StackInfo.itemStackToNBT(stack, false);
        if (nbt == null) throw new ToolException("Item has no registry name");
        for (String key : FLUID_KEYS) if (nbt.hasKey(key)) return FLUID + nbt.getString(key);
        nbt.removeTag("Count");
        if (!plain(nbt) && nbt.toString()
            .length() > MAX_NBT) nbt.removeTag("tag");
        if (!plain(nbt)) return nbt.toString();
        int damage = nbt.getInteger("Damage");
        return damage == 0 ? nbt.getString("strId") : nbt.getString("strId") + ":" + damage;
    }

    @SuppressWarnings("unchecked")
    private static boolean plain(NBTTagCompound nbt) {
        return PLAIN.containsAll(nbt.func_150296_c());
    }

    public ItemStack resolve(String idOrQuery) {
        ItemStack stack = parse(idOrQuery);
        return stack != null ? stack : search(idOrQuery);
    }

    /**
     * NEI's fluid display stacks are named after their fluid; a filled container keeps its own name with the contents
     * after it, e.g. {@code Large Aluminium Fluid Cell (4432 mB Molten Polyethylene)}, since its id is the container's.
     */
    public String name(ItemStack stack) {
        FluidStack fluid = StackInfo.getFluid(stack);
        if (fluid == null) return clean(stack.getDisplayName());
        String fluidName = clean(fluid.getLocalizedName());
        if (StackInfo.isFluidDisplayItem(stack)) return fluidName;
        String name = clean(stack.getDisplayName());
        // GT cells already name their fluid ("Ethylene Cell"); generic containers do not.
        return name.contains(fluidName) ? name : name + " (" + fluid.amount + " mB " + fluidName + ")";
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

    private static ItemStack parse(String id) {
        if (id.startsWith(FLUID)) {
            for (String key : FLUID_KEYS) {
                NBTTagCompound nbt = new NBTTagCompound();
                nbt.setString(key, id.substring(FLUID.length()));
                ItemStack stack = load(nbt);
                if (stack != null) return stack;
            }
            return null;
        }
        NBTTagCompound nbt = new NBTTagCompound();
        try {
            if (id.startsWith("{")) nbt = (NBTTagCompound) JsonToNBT.func_150315_a(id);
            else {
                int last = id.lastIndexOf(':');
                boolean meta = last > id.indexOf(':') && id.substring(last + 1)
                    .matches("\\d+");
                nbt.setString("strId", meta ? id.substring(0, last) : id);
                nbt.setInteger("Damage", meta ? Integer.parseInt(id.substring(last + 1)) : 0);
            }
        } catch (Exception e) {
            return null;
        }
        return load(nbt);
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
