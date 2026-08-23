package dev.gtnhmcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;

public final class BetterQuestingProvider {

    public JsonObject query(JsonObject p, EntityPlayer player) {
        String q = text(p, "query").toLowerCase(Locale.ROOT), id = text(p, "id"), chapter = text(p, "chapter_id");
        int off = number(p, "offset", 0, 0, 100000), lim = number(p, "limit", 50, 1, 200);
        Set<String> states = new HashSet<String>();
        if (p.has("states") && p.get("states")
            .isJsonArray()) for (JsonElement state : p.getAsJsonArray("states"))
                states.add(
                    state.getAsString()
                        .toUpperCase(Locale.ROOT));
        Map<UUID, List<String>> chapters = chapters();
        JsonArray a = new JsonArray();
        int n = 0, counted = 0, unlocked = 0, complete = 0, claimed = 0;
        UUID user = player.getUniqueID();
        for (java.util.Iterator<Map.Entry<UUID, IQuest>> it = QuestDatabase.INSTANCE.orderedEntries()
            .iterator(); it.hasNext();) {
            Map.Entry<UUID, IQuest> e = it.next();
            IQuest x = e.getValue();
            String name = x.getProperty(NativeProps.NAME, "");
            String desc = x.getProperty(NativeProps.DESC, "");
            String state = x.getState(player)
                .name();
            List<String> questChapters = chapters.get(e.getKey());
            if ((!id.isEmpty() && !id.equals(
                e.getKey()
                    .toString()))
                || (!chapter.isEmpty() && (questChapters == null || !questChapters.contains(chapter)))
                || (!states.isEmpty() && !states.contains(state.toUpperCase(Locale.ROOT)))
                || (!q.isEmpty() && !name.toLowerCase(Locale.ROOT)
                    .contains(q)
                    && !desc.toLowerCase(Locale.ROOT)
                        .contains(q)))
                continue;
            // Totals intentionally cover the complete filtered result, not this page.
            if (Boolean.TRUE.equals(x.getProperty(NativeProps.COUNT_AS_QUEST, true))) {
                counted++;
                if (x.isUnlocked(user)) unlocked++;
                if (x.isComplete(user)) complete++;
                if (x.hasClaimed(user)) claimed++;
            }
            if (n++ < off) continue;
            if (a.size() >= lim) continue;
            JsonObject o = new JsonObject();
            o.addProperty(
                "id",
                e.getKey()
                    .toString());
            o.addProperty("name", name);
            o.addProperty("description", desc.length() > 1024 ? desc.substring(0, 1024) : desc);
            o.addProperty("state", state);
            o.addProperty("unlocked", x.isUnlocked(player.getUniqueID()));
            o.addProperty("complete", x.isComplete(player.getUniqueID()));
            o.addProperty("claimed", x.hasClaimed(player.getUniqueID()));
            JsonArray r = new JsonArray();
            for (UUID requirement : x.getRequirements()) r.add(new JsonPrimitive(requirement.toString()));
            o.add("requirements", r);
            JsonArray cs = new JsonArray();
            if (questChapters != null) for (String line : questChapters) cs.add(new JsonPrimitive(line));
            o.add("chapters", cs);
            a.add(o);
        }
        JsonObject d = new JsonObject();
        d.add("quests", a);
        d.addProperty("offset", off);
        d.addProperty("limit", lim);
        boolean truncated = n > off + a.size();
        if (truncated) d.addProperty("next_offset", off + a.size());
        d.addProperty("truncated", truncated);
        JsonObject totals = new JsonObject();
        totals.addProperty("counted", counted);
        totals.addProperty("unlocked", unlocked);
        totals.addProperty("completed", complete);
        totals.addProperty("claimed", claimed);
        totals.addProperty("unlocked_percent", percent(unlocked, counted));
        totals.addProperty("completed_percent", percent(complete, counted));
        totals.addProperty("claimed_percent", percent(claimed, counted));
        d.add("totals", totals);
        return d;
    }

    private Map<UUID, List<String>> chapters() {
        Map<UUID, List<String>> out = new HashMap<UUID, List<String>>();
        for (Map.Entry<UUID, IQuestLine> line : QuestLineDatabase.INSTANCE.getOrderedEntries())
            for (UUID quest : line.getValue()
                .keySet()) {
                    List<String> lines = out.get(quest);
                    if (lines == null) {
                        lines = new ArrayList<String>();
                        out.put(quest, lines);
                    }
                    lines.add(
                        line.getKey()
                            .toString());
                }
        return out;
    }

    private static String text(JsonObject p, String key) {
        return p.has(key) && p.get(key)
            .isJsonPrimitive() ? p.get(key)
                .getAsString() : "";
    }

    private static int number(JsonObject p, String key, int d, int lo, int hi) {
        if (!p.has(key)) return d;
        int n = p.get(key)
            .getAsInt();
        if (n < lo || n > hi) throw new IllegalArgumentException(key + " outside range");
        return n;
    }

    private static double percent(int n, int total) {
        return total == 0 ? 0.0 : (100.0 * n / total);
    }
}
