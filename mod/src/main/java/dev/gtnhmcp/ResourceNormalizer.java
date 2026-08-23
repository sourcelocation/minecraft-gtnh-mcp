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
        return fluid == null ? item(stack, amount, player, tooltipLines, false) : fluid(fluid, amount, false);
    }

    public static JsonObject identifiedResource(ItemStack stack, long amount, EntityPlayer player, int tooltipLines) {
        FluidStack fluid = StackInfo.getFluid(stack);
        return fluid == null ? item(stack, amount, player, tooltipLines, true) : fluid(fluid, amount, true);
    }

    public static JsonObject exactItem(ItemStack stack, EntityPlayer player) {
        JsonObject result = item(stack, stack.stackSize, player, 2, false);
        result.addProperty("item_id", itemID(stack));
        result.addProperty("registry", registry(stack));
        result.addProperty("meta", stack.getItemDamage());
        result.add("tooltip", tooltip(stack, player, Integer.MAX_VALUE, false, true));
        result.add("ore_names", oreNames(stack));
        return result;
    }

    public static String itemID(ItemStack stack) {
        int meta = stack.getItem()
            .getHasSubtypes() ? stack.getItemDamage() : 0;
        return registry(stack) + ":" + meta;
    }

    public static String resourceID(ItemStack stack) {
        FluidStack fluid = StackInfo.getFluid(stack);
        return fluid == null ? itemID(stack)
            : fluid.getFluid()
                .getName();
    }

    private static JsonObject item(ItemStack stack, long amount, EntityPlayer player, int tooltipLines,
        boolean includeID) {
        JsonObject result = new JsonObject();
        result.addProperty("kind", "item");
        if (includeID) result.addProperty("id", itemID(stack));
        result.addProperty("amount", amount);
        result.addProperty("text", formattedItem(stack, player, tooltipLines));
        return result;
    }

    private static JsonObject fluid(FluidStack stack, long amount, boolean includeID) {
        JsonObject result = new JsonObject();
        result.addProperty("kind", "fluid");
        if (includeID) result.addProperty(
            "id",
            stack.getFluid()
                .getName());
        result.addProperty("amount", amount);
        result.addProperty("text", clean(stack.getLocalizedName()));
        return result;
    }

    private static String formattedItem(ItemStack stack, EntityPlayer player, int tooltipLines) {
        StringBuilder result = new StringBuilder(clean(stack.getDisplayName()));
        result.append(" [")
            .append(modID(stack))
            .append(']');
        JsonArray lines = tooltip(stack, player, tooltipLines, true, false);
        for (int i = 0; i < lines.size(); i++) result.append('\n')
            .append(
                lines.get(i)
                    .getAsString());
        return result.toString();
    }

    private static JsonArray tooltip(ItemStack stack, EntityPlayer player, int limit, boolean skipName,
        boolean advanced) {
        JsonArray result = new JsonArray();
        if (player == null || limit <= 0) return result;
        try {
            List<String> lines = stack.getTooltip(player, advanced);
            int start = skipName && !lines.isEmpty() ? 1 : 0;
            for (int i = start; i < lines.size() && result.size() < limit; i++) {
                String line = clean(lines.get(i));
                if (!line.isEmpty()) result.add(new JsonPrimitive(line));
            }
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

    private static String modID(ItemStack stack) {
        String name = registry(stack);
        int colon = name.indexOf(':');
        return colon < 0 ? name : name.substring(0, colon);
    }

    private static String clean(String value) {
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(value == null ? "" : value);
        return clean == null ? ""
            : clean.trim()
                .replaceAll("\\s+", " ");
    }
}
