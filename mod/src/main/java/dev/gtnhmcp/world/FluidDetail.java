package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Stack;
import dev.gtnhmcp.core.ToolException;

/** Fluids in any Forge fluid handler's tanks, as far as the server syncs them to the client. */
final class FluidDetail implements BlockDetail {

    private final Items items;

    FluidDetail(Items items) {
        this.items = items;
    }

    public void describe(BlockAt block, Map<String, Object> details) {
        if (!(block.tile() instanceof IFluidHandler handler)) return;
        FluidTankInfo[] tanks = handler.getTankInfo(ForgeDirection.UNKNOWN);
        if (tanks == null) return;
        List<Stack> fluids = new ArrayList<>();
        for (FluidTankInfo tank : tanks) {
            if (tank == null || tank.fluid == null || tank.fluid.amount <= 0) continue;
            try {
                fluids.add(items.fluid(tank.fluid));
            } catch (ToolException noDisplay) {
                // A fluid NEI cannot show is left out rather than failing the whole listing.
            }
        }
        details.put("fluids", fluids);
    }
}
