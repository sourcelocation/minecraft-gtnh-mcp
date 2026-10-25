package dev.gtnhmcp.item;

import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhmcp.core.Unit;

/** {@code mod:name} or {@code mod:name:meta}: items whose NBT is only their registry name and damage. */
final class RegistryId implements IdFormat {

    private static final Set<String> KEYS = Set.of("strId", "Damage");

    @SuppressWarnings("unchecked")
    public String encode(NBTTagCompound nbt) {
        if (!KEYS.containsAll(nbt.func_150296_c())) return null;
        int damage = nbt.getInteger("Damage");
        return damage == 0 ? nbt.getString("strId") : nbt.getString("strId") + ":" + damage;
    }

    /** The last segment is the meta when there are two colons and it is a number. */
    public NBTTagCompound decode(String id) {
        int last = id.lastIndexOf(':');
        boolean meta = last > id.indexOf(':') && id.substring(last + 1)
            .matches("\\d+");
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("strId", meta ? id.substring(0, last) : id);
        nbt.setInteger("Damage", meta ? Integer.parseInt(id.substring(last + 1)) : 0);
        return nbt;
    }

    public Unit unit() {
        return Unit.ITEMS;
    }
}
