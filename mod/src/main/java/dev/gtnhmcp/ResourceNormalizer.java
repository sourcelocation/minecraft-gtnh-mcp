package dev.gtnhmcp;

import java.nio.charset.Charset;
import java.security.MessageDigest;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.registry.GameData;

/** Canonical, stable resource wire identity shared by player and NEI providers. */
public final class ResourceNormalizer {

    private ResourceNormalizer() {}

    public static JsonObject item(ItemStack s, long amount) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", "item");
        String registry = (String) GameData.getItemRegistry()
            .getNameForObject(s.getItem());
        o.addProperty(
            "registry",
            registry == null ? s.getItem()
                .getUnlocalizedName() : registry);
        o.addProperty("meta", s.getItemDamage());
        o.addProperty("display", s.getDisplayName());
        o.addProperty("amount", amount);
        String guid = StackInfo.getItemStackGUID(s);
        o.addProperty(
            "id",
            guid == null ? sha(
                o.get("registry")
                    .getAsString() + ":"
                    + s.getItemDamage())
                : guid);
        if (s.hasTagCompound()) o.addProperty(
            "nbt_hash",
            sha(
                s.getTagCompound()
                    .toString()));
        JsonArray ores = new JsonArray();
        for (int id : OreDictionary.getOreIDs(s)) ores.add(new JsonPrimitive(OreDictionary.getOreName(id)));
        o.add("ore_names", ores);
        return o;
    }

    public static JsonObject fluid(FluidStack f) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", "fluid");
        o.addProperty(
            "id",
            f.getFluid()
                .getName());
        o.addProperty(
            "registry",
            f.getFluid()
                .getName());
        o.addProperty("display", f.getLocalizedName());
        o.addProperty("amount", (long) f.amount);
        return o;
    }

    public static JsonObject resource(ItemStack s, long amount) {
        FluidStack f = StackInfo.getFluid(s);
        if (f == null) return item(s, amount);
        FluidStack copy = f.copy();
        copy.amount = (int) Math.min(Integer.MAX_VALUE, amount);
        return fluid(copy);
    }

    public static String sha(String value) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(Charset.forName("UTF-8")));
            StringBuilder s = new StringBuilder();
            for (byte x : b) s.append(String.format("%02x", x & 255));
            return s.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
