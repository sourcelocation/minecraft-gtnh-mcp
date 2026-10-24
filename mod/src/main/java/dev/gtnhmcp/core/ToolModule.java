package dev.gtnhmcp.core;

import java.util.List;

/** The tools for one data source, e.g. NEI recipes or BetterQuesting. */
public interface ToolModule extends Extension {

    List<Tool<?, ?>> tools(Game game);
}
