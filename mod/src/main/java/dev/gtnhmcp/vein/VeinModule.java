package dev.gtnhmcp.vein;

import java.util.List;

import cpw.mods.fml.common.Loader;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolModule;

/** Kept free of VisualProspecting types so it loads, and reports unavailable, when the mod is absent. */
public final class VeinModule implements ToolModule {

    public boolean available() {
        return Loader.isModLoaded("visualprospecting");
    }

    public List<Tool<?, ?>> tools(Game game) {
        return List.of(new FindVeins(game));
    }
}
