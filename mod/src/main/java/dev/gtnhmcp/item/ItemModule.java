package dev.gtnhmcp.item;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import codechicken.nei.ItemList;
import codechicken.nei.SearchField;
import codechicken.nei.api.ItemFilter;
import dev.gtnhmcp.core.AbstractTool;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Items;
import dev.gtnhmcp.core.Page;
import dev.gtnhmcp.core.Tool;
import dev.gtnhmcp.core.ToolException;
import dev.gtnhmcp.core.ToolModule;

public final class ItemModule implements ToolModule {

    public List<Tool<?, ?>> tools(Game game) {
        return List.of(new FindItems(game.items()));
    }

    public record Query(@JsonProperty(
        required = true) @JsonPropertyDescription("NEI search, e.g. 'iron plate', '@gregtech', '$plateIron'") String query,
        String cursor, Integer limit) {}

    public record Item(String id, String name) {}

    static final class FindItems extends AbstractTool<Query, Page<Item>> {

        private final Items items;

        FindItems(Items items) {
            super("find_items", "Search NEI's item list. Returns ids accepted by every other tool.");
            this.items = items;
        }

        public Page<Item> call(Query q) {
            if (!ItemList.loadFinished) throw new ToolException("NEI is still loading items, retry shortly");
            ItemFilter filter = SearchField.getFilter(q.query());
            return Page.of(
                ItemList.items.stream()
                    .filter(filter::matches)
                    .map(s -> new Item(items.id(s), items.name(s))),
                q.cursor(),
                q.limit());
        }
    }
}
