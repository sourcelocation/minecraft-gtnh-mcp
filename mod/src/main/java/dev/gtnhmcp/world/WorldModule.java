package dev.gtnhmcp.world;

import java.util.List;

import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolModule;

/** The blocks around the player, and named regions the model can keep notes on. */
public final class WorldModule implements ToolModule {

    public List<Tool<?, ?>> tools(Game game) {
        LoadedBlocks blocks = new LoadedBlocks(new BlockNames(game.items()));
        List<BlockDetail> details = List.of(new InventoryDetail(game.items()), new FluidDetail(game.items()));
        RegionStore regions = new RegionStore();
        return List.of(
            new BlockTools.GetBlocks(game, blocks, details),
            new BlockTools.FindBlocks(game, blocks, details),
            new RegionTools.LabelRegion(game, regions),
            new RegionTools.DeleteRegion(regions),
            new RegionTools.ListRegions(game, regions));
    }
}
