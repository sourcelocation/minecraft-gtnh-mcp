package dev.gtnhmcp.quest;

import java.util.List;

import cpw.mods.fml.common.Loader;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolModule;

/** Kept free of BetterQuesting types so it loads, and reports unavailable, when the mod is absent. */
public final class QuestModule implements ToolModule {

    public boolean available() {
        return Loader.isModLoaded("betterquesting");
    }

    public List<Tool<?, ?>> tools(Game game) {
        QuestBook book = new QuestBook(game.items());
        return List.of(new QuestTools.FindQuests(game, book), new QuestTools.GetQuest(game, book));
    }
}
