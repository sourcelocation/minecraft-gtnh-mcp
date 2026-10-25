package dev.gtnhmcp.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import net.minecraft.entity.player.EntityPlayer;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import bq_standard.tasks.ITaskItemInput;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Stack;
import dev.gtnhmcp.core.Text;
import dev.gtnhmcp.core.ToolException;

/** Read access to BetterQuesting through its public API, with translated names. */
final class QuestBook {

    private final Items items;

    QuestBook(Items items) {
        this.items = items;
    }

    Stream<Map.Entry<UUID, IQuest>> quests() {
        return QuestingAPI.getAPI(ApiReference.QUEST_DB)
            .orderedEntries();
    }

    IQuest quest(UUID id) {
        IQuest quest = QuestingAPI.getAPI(ApiReference.QUEST_DB)
            .get(id);
        if (quest == null) throw new ToolException("No quest " + id);
        return quest;
    }

    /** Chapter names per quest, in chapter order. */
    Map<UUID, List<String>> chapters() {
        Map<UUID, List<String>> chapters = new HashMap<>();
        for (Map.Entry<UUID, IQuestLine> line : QuestingAPI.getAPI(ApiReference.LINE_DB)
            .getOrderedEntries()) {
            String name = Text.clean(QuestTranslation.translateQuestLineName(line));
            for (UUID quest : line.getValue()
                .keySet())
                chapters.computeIfAbsent(quest, k -> new ArrayList<>())
                    .add(name);
        }
        return chapters;
    }

    String name(UUID id, IQuest quest) {
        return Text.clean(QuestTranslation.translateQuestName(id, quest));
    }

    String description(UUID id, IQuest quest) {
        return Text.clean(QuestTranslation.translateQuestDescription(id, quest));
    }

    String state(IQuest quest, EntityPlayer player) {
        return quest.getState(player)
            .name()
            .toLowerCase(Locale.ROOT);
    }

    /**
     * The items the quest's tasks ask for, through BetterQuesting's item-input interface (fluid tasks are left out).
     */
    List<Stack> needs(IQuest quest) {
        return quest.getTasks()
            .getEntries()
            .stream()
            .map(DBEntry::getValue)
            .filter(task -> task instanceof ITaskItemInput)
            .flatMap(
                task -> ((ITaskItemInput) task).getItemInputs()
                    .stream())
            .map(item -> items.stack(item.getBaseStack(), item.stackSize))
            .toList();
    }
}
