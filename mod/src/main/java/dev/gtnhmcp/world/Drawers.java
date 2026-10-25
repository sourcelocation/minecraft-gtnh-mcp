package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;

import cpw.mods.fml.common.Loader;
import dev.gtnhmcp.core.Items;

/** A drawer by what it holds, e.g. {@code Drawer: 2304× Sodium Dust}; drawers sync their contents to the client. */
public final class Drawers implements BlockDescriber {

    private static final int SHOWN = 4;

    public boolean available() {
        return Loader.isModLoaded("StorageDrawers");
    }

    public String describe(TileEntity tile, Items items) {
        if (!(tile instanceof IDrawerGroup group)) return null;
        List<String> contents = new ArrayList<>();
        for (int slot = 0; slot < group.getDrawerCount(); slot++) {
            IDrawer drawer = group.isDrawerEnabled(slot) ? group.getDrawer(slot) : null;
            if (drawer != null && !drawer.isEmpty())
                contents.add(drawer.getStoredItemCount() + "× " + items.name(drawer.getStoredItemPrototype()));
        }
        if (contents.isEmpty()) return "Drawer (empty)";
        if (contents.size() > SHOWN) {
            int more = contents.size() - SHOWN;
            contents = new ArrayList<>(contents.subList(0, SHOWN));
            contents.add("+" + more + " more");
        }
        return "Drawer: " + String.join(", ", contents);
    }
}
