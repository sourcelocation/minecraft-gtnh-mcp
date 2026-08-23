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
        FluidStack fluid = StackInfo.getFluid(stack);
        return fluid == null ? item(stack, amount, player, tooltipLines) : fluid(fluid, amount);
    }

    public static JsonObject planningResource(ItemStack stack, long amount, EntityPlayer player, int tooltipLines) {
        FluidStack fluid = StackInfo.getFluid(stack);
        JsonObject result = resource(stack, amount, player, tooltipLines);
        result.addProperty("kind", fluid == null ? "item" : "fluid");
        result.addProperty("amount", amount);
        return result;
    }

    public static JsonObject exactItem(ItemStack stack, EntityPlayer player) {
        JsonObject result = new JsonObject();
        result.addProperty("id", itemID(stack));
        result.addProperty("item", formattedItem(stack, stack.stackSize, player, Integer.MAX_VALUE, true));
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

    private static JsonObject item(ItemStack stack, long amount, EntityPlayer player, int tooltipLines) {
        JsonObject result = new JsonObject();
        result.addProperty("id", itemID(stack));
        result.addProperty("item", formattedItem(stack, amount, player, tooltipLines, false));
        return result;
    }

    private static JsonObject fluid(FluidStack stack, long amount) {
        JsonObject result = new JsonObject();
        result.addProperty(
            "id",
            stack.getFluid()
                .getName());
        result.addProperty("item", amount + "× " + clean(stack.getLocalizedName()));
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
}
