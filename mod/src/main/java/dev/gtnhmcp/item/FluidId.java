package dev.gtnhmcp.item;

import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhmcp.core.Unit;

/** {@code fluid:water}: NEI's fluid display stacks, which NEI writes as {@code {neiFluidName:"water"}}. */
final class FluidId implements IdFormat {

    private static final String PREFIX = "fluid:";
    private static final String KEY = "neiFluidName";

    public String encode(NBTTagCompound nbt) {
        return nbt.hasKey(KEY) ? PREFIX + nbt.getString(KEY) : null;
    }

    public NBTTagCompound decode(String id) {
        if (!id.startsWith(PREFIX)) return null;
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString(KEY, id.substring(PREFIX.length()));
        return nbt;
    }

    public Unit unit() {
        return Unit.MILLIBUCKETS;
    }
}
