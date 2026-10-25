package dev.gtnhmcp.item;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhmcp.core.Unit;

/** {@code {strId:"...",tag:{...}}}: NEI's NBT as is, for items that differ by tag, e.g. enchanted armor. */
final class NbtId implements IdFormat {

    public String encode(NBTTagCompound nbt) {
        return nbt.toString();
    }

    public NBTTagCompound decode(String id) {
        if (!id.startsWith("{")) return null;
        try {
            return JsonToNBT.func_150315_a(id) instanceof NBTTagCompound nbt ? nbt : null;
        } catch (NBTException e) {
            return null;
        }
    }

    public Unit unit() {
        return Unit.ITEMS;
    }
}
