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
        return id.startsWith(PREFIX) ? nbt(id.substring(PREFIX.length())) : null;
    }

    public Unit unit() {
        return Unit.MILLIBUCKETS;
    }

    /** NEI's NBT for a fluid's display stack, by the fluid's registry name. */
    NBTTagCompound nbt(String fluidName) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString(KEY, fluidName);
        return nbt;
    }
}
