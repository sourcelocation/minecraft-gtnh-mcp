package dev.gtnhmcp.world;

import net.minecraft.tileentity.TileEntity;

import cpw.mods.fml.common.Loader;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Text;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * Every GregTech machine and pipe is the same block; its meta tile entity says which machine it is. Running state is
 * synced to the client, so it is reported as "(running)".
 */
public final class GregTechMachines implements BlockDescriber {

    public boolean available() {
        return Loader.isModLoaded("gregtech");
    }

    public String describe(TileEntity tile, Items items) {
        if (!(tile instanceof IGregTechTileEntity machine) || machine.getMetaTileEntity() == null) return null;
        String name = Text.clean(
            machine.getMetaTileEntity()
                .getLocalName());
        return machine.isActive() ? name + " (running)" : name;
    }
}
