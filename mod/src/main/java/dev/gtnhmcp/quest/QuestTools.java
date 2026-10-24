package dev.gtnhmcp.quest;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import betterquesting.api.questing.IQuest;
import dev.gtnhmcp.core.AbstractTool;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Page;
import dev.gtnhmcp.core.ToolException;

final class QuestTools {

    private QuestTools() {}

    public record Search(@JsonPropertyDescription("text in name or description") String query,
        @JsonPropertyDescription("locked, unlocked, unclaimed or completed") String state,
        @JsonPropertyDescription("text in the chapter name") String chapter, String cursor, Integer limit) {}

    public record Quest(String id, String name, String state, List<String> chapters) {}

    public record Quests(List<Quest> items, String next,
        @JsonPropertyDescription("completed of total, ignoring the state filter") int completed, int total) {}

    public record QuestRef(@JsonProperty(required = true) String id) {}

    public record QuestDetail(String id, String name, String state, String description, List<String> chapters,
        List<Quest> requires) {}

    static final class FindQuests extends AbstractTool<Search, Quests> {

        private final Game game;
        private final QuestBook book;

        FindQuests(Game game, QuestBook book) {
            super("find_quests", "Search BetterQuesting quests with the player's progress.");
            this.game = game;
            this.book = book;
        }

        public Quests call(Search s) {
            EntityPlayer player = game.player();
            Map<UUID, List<String>> chapters = book.chapters();
            List<Quest> matches = book.quests()
                .filter(
                    e -> matches(s.query(), book.name(e.getKey(), e.getValue()))
                        || matches(s.query(), book.description(e.getKey(), e.getValue())))
                .map(
                    e -> new Quest(
                        e.getKey()
                            .toString(),
                        book.name(e.getKey(), e.getValue()),
                        book.state(e.getValue(), player),
                        chapters.getOrDefault(e.getKey(), List.of())))
                .filter(
                    q -> s.chapter() == null || q.chapters()
                        .stream()
                        .anyMatch(contains(s.chapter())))
                .toList();
            // Progress covers the query and chapter, not the state filter, so it stays meaningful.
            int completed = (int) matches.stream()
                .filter(q -> "completed".equals(q.state()))
                .count();
            Page<Quest> page = Page.of(
                matches.stream()
                    .filter(
                        q -> s.state() == null || q.state()
                            .equalsIgnoreCase(s.state())),
                s.cursor(),
                s.limit());
            return new Quests(page.items(), page.next(), completed, matches.size());
        }

        private static boolean matches(String query, String text) {
            return query == null || contains(query).test(text);
        }
    }

    static final class GetQuest extends AbstractTool<QuestRef, QuestDetail> {

        private final Game game;
        private final QuestBook book;

        GetQuest(Game game, QuestBook book) {
            super("get_quest", "One quest's description, chapters and prerequisites.");
            this.game = game;
            this.book = book;
        }

        public QuestDetail call(QuestRef ref) {
            EntityPlayer player = game.player();
            UUID id = uuid(ref.id());
            IQuest quest = book.quest(id);
            Map<UUID, List<String>> chapters = book.chapters();
            List<Quest> requires = quest.getRequirements()
                .stream()
                .map(r -> {
                    IQuest required = book.quest(r);
                    return new Quest(r.toString(), book.name(r, required), book.state(required, player), null);
                })
                .toList();
            return new QuestDetail(
                ref.id(),
                book.name(id, quest),
                book.state(quest, player),
                book.description(id, quest),
                chapters.getOrDefault(id, List.of()),
                requires);
        }

        private static UUID uuid(String id) {
            try {
                return UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                throw new ToolException("Quest ids are UUIDs from find_quests");
            }
        }
    }

    private static Predicate<String> contains(String needle) {
        String lower = needle.toLowerCase(Locale.ROOT);
        return text -> text.toLowerCase(Locale.ROOT)
            .contains(lower);
    }
}
