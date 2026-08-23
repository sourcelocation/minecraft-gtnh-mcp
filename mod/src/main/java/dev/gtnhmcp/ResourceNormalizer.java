package dev.gtnhmcp;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.registry.GameData;

/** Shared compact, human-readable representation for every item-bearing response. */
public final class ResourceNormalizer {

    private ResourceNormalizer() {}

    public static JsonObject resource(ItemStack stack, long amount, EntityPlayer player, int tooltipLines) {
        return resource(stack, amount, player, ItemFormat.compact(tooltipLines));
    }

    public static JsonObject resource(ItemStack stack, long amount, EntityPlayer player, ItemFormat format) {
        FluidStack fluid = StackInfo.getFluid(stack);
        return fluid == null ? item(stack, amount, player, format) : fluid(fluid, amount, format);
    }

    public static JsonObject exactItem(ItemStack stack, EntityPlayer player) {
        ItemFormat format = ItemFormat.detailed();
        JsonObject result = new JsonObject();
        result.addProperty("id", itemID(stack));
        result.addProperty("item", formattedItem(stack, stack.stackSize, player, format.tooltipLines, format.advanced));
        if (stack.hasTagCompound()) result.addProperty(
            "nbt",
            stack.getTagCompound()
                .toString());
        JsonArray ores = oreNames(stack);
        if (ores.size() > 0) result.add("ore_names", ores);
        return result;
    }

    public static String itemID(ItemStack stack) {
        int meta = stack.getItem()
            .getHasSubtypes() ? stack.getItemDamage() : 0;
        return registry(stack) + (meta == 0 ? "" : ":" + meta);
    }

    public static String resourceID(ItemStack stack) {
        FluidStack fluid = StackInfo.getFluid(stack);
        return fluid == null ? itemID(stack)
            : fluid.getFluid()
                .getName();
    }

    private static JsonObject item(ItemStack stack, long amount, EntityPlayer player, ItemFormat format) {
        JsonObject result = new JsonObject();
        result.addProperty("id", itemID(stack));
        result.addProperty("item", formattedItem(stack, amount, player, format.tooltipLines, format.advanced));
        if (format.chance != null && format.chance.doubleValue() != 1.0) result.addProperty("chance", format.chance);
        return result;
    }

    private static JsonObject fluid(FluidStack stack, long amount, ItemFormat format) {
        JsonObject result = new JsonObject();
        result.addProperty(
            "id",
            stack.getFluid()
                .getName());
        result.addProperty("item", amount + "× " + clean(stack.getLocalizedName()));
        if (format.chance != null && format.chance.doubleValue() != 1.0) result.addProperty("chance", format.chance);
        return result;
    }

    private static String formattedItem(ItemStack stack, long amount, EntityPlayer player, int tooltipLines,
        boolean advanced) {
        JsonArray lines = tooltip(stack, player, tooltipLines, !advanced, advanced);
        StringBuilder result = new StringBuilder(Long.toString(amount));
        result.append("× ")
            .append(
                advanced && lines.size() > 0 ? lines.get(0)
                    .getAsString() : clean(stack.getDisplayName()));
        int start = advanced && lines.size() > 0 ? 1 : 0;
        for (int i = start; i < lines.size(); i++) result.append('\n')
            .append(
                lines.get(i)
                    .getAsString());
        return result.toString();
    }

    private static JsonArray tooltip(ItemStack stack, EntityPlayer player, int limit, boolean skipName,
        boolean advanced) {
        JsonArray result = new JsonArray();
        if (player == null) return result;
        try {
            List<String> lines = stack.getTooltip(player, advanced);
            int start = skipName && !lines.isEmpty() ? 1 : 0;
            boolean truncated = false;
            for (int i = start; i < lines.size(); i++) {
                String line = clean(lines.get(i));
                if (line.isEmpty()) continue;
                if (result.size() < limit) result.add(new JsonPrimitive(line));
                else truncated = true;
            }
            if (skipName && truncated) result.add(new JsonPrimitive("… (more tooltip lines)"));
        } catch (RuntimeException ignored) {}
        return result;
    }

    private static JsonArray oreNames(ItemStack stack) {
        JsonArray result = new JsonArray();
        for (int id : OreDictionary.getOreIDs(stack)) result.add(new JsonPrimitive(OreDictionary.getOreName(id)));
        return result;
    }

    private static String registry(ItemStack stack) {
        String name = (String) GameData.getItemRegistry()
            .getNameForObject(stack.getItem());
        return name == null ? stack.getItem()
            .getUnlocalizedName() : name;
    }

    private static String clean(String value) {
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(value == null ? "" : value);
        return clean == null ? ""
            : clean.trim()
                .replaceAll("\\s+", " ");
    }

    /** Controls the human-readable item text without changing its stable resource ID. */
    public static final class ItemFormat {

        private final int tooltipLines;
        private final boolean advanced;
        private final Double chance;

        private ItemFormat(int tooltipLines, boolean advanced, Double chance) {
            this.tooltipLines = tooltipLines;
            this.advanced = advanced;
            this.chance = chance;
        }

        public static ItemFormat compact(int tooltipLines) {
            return new ItemFormat(tooltipLines, false, null);
        }

        public static ItemFormat detailed() {
            return new ItemFormat(Integer.MAX_VALUE, true, null);
        }

        public ItemFormat withTooltipLines(int tooltipLines) {
            return new ItemFormat(tooltipLines, advanced, chance);
        }

        public ItemFormat withAdvancedTooltip(boolean advanced) {
            return new ItemFormat(tooltipLines, advanced, chance);
        }

        public ItemFormat withChance(double chance) {
            return new ItemFormat(tooltipLines, advanced, chance);
        }
    }
}
