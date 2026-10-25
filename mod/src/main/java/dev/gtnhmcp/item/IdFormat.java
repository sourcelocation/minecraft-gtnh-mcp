package dev.gtnhmcp.item;

import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhmcp.core.Unit;

/** One way to write NEI's stack NBT as a short id, and back; {@link NeiItems} tries each in order. */
interface IdFormat {

    /** The id for this NBT (without a count), or null when this format does not apply. */
    String encode(NBTTagCompound nbt);

    /** The NBT for this id, or null when the id is not in this format. */
    NBTTagCompound decode(String id);

    /** How amounts of things with ids in this format are counted. */
    Unit unit();
}
