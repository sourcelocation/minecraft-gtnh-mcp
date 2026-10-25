package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.AbstractTool;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.ToolException;

/**
 * Reading blocks: an overview of an area with chosen blocks listed in full, matching blocks across a larger one, or
 * one.
 */
final class BlockTools {

    private static final int LISTED_UP_TO = 100;

    private BlockTools() {}

    /** What a listed block gets: its contents always, its neighbours unless turned off. */
    record Details(List<BlockDetail> contents, BlockDetail neighbours) {

        List<BlockDetail> pick(Boolean withNeighbours) {
            if (Boolean.FALSE.equals(withNeighbours)) return contents;
            List<BlockDetail> all = new ArrayList<>(contents);
            all.add(neighbours);
            return all;
        }
    }

    public record Area(@JsonPropertyDescription("blocks around the player, default 5, max 32") Integer radius,
        @JsonPropertyDescription("corner [x, y, z]; with to, used instead of radius") List<Integer> from,
        @JsonPropertyDescription("opposite corner [x, y, z]") List<Integer> to,
        @JsonPropertyDescription("text in block names from the overview to also list one by one with position and "
            + "details") String expand,
        @JsonPropertyDescription("list each expanded block's adjacent blocks, default true") Boolean neighbours) {}

    public record Search(@JsonProperty(
        required = true) @JsonPropertyDescription("text in the block name, e.g. 'blast furnace', 'drawer'") String query,
        @JsonPropertyDescription("blocks around the player, default 32, max 64") Integer radius,
        @JsonPropertyDescription("corner [x, y, z]; with to, used instead of radius") List<Integer> from,
        @JsonPropertyDescription("opposite corner [x, y, z]") List<Integer> to,
        @JsonPropertyDescription("list each block's adjacent blocks, default true") Boolean neighbours) {}

    public record Position(@JsonProperty(required = true) @JsonPropertyDescription("[x, y, z]") List<Integer> at,
        @JsonPropertyDescription("list its adjacent blocks, default true") Boolean neighbours) {}

    public record Blocks(@JsonPropertyDescription("by name, rarest first") Map<String, Overview.Group> blocks,
        @JsonPropertyDescription("the blocks matching expand, one by one") List<Listing.Entry> expanded,
        @JsonPropertyDescription("matching blocks past the listing limit by name; ask again for fewer names") Map<String, Integer> unlisted,
        @JsonPropertyDescription("true when part of the area is in unloaded chunks and was skipped") boolean partial) {}

    public record Found(List<Listing.Entry> blocks,
        @JsonPropertyDescription("matching blocks past the listing limit by name; narrow the query or area") Map<String, Integer> unlisted,
        @JsonPropertyDescription("true when part of the area is in unloaded chunks and was skipped") boolean partial) {}

    static final class GetBlocks extends AbstractTool<Area, Blocks> {

        private static final int MAX_SIDE = 65;
        private static final int POSITIONS_UP_TO = 8;

        private final Game game;
        private final LoadedBlocks blocks;
        private final Details details;

        GetBlocks(Game game, LoadedBlocks blocks, Details details) {
            super(
                "get_blocks",
                "Blocks around",
                "Every non-air block in an area grouped by name (machines by machine). Then call again with expand "
                    + "set to names from it to list those blocks with position, inventory, fluids and neighbours.");
            this.game = game;
            this.blocks = blocks;
            this.details = details;
        }

        public Blocks call(Area area) {
            EntityPlayer player = game.player();
            Box box = Box.of(player, area.radius(), area.from(), area.to(), 5)
                .limit(MAX_SIDE);
            Overview overview = new Overview(POSITIONS_UP_TO);
            List<BlockSink> sinks = new ArrayList<>(List.of(overview));
            Listing listing = area.expand() == null ? null
                : new Listing(area.expand(), LISTED_UP_TO, details.pick(area.neighbours()));
            if (listing != null) sinks.add(listing);
            boolean partial = blocks.walk(player.worldObj, box, sinks);
            return listing == null ? new Blocks(overview.groups(), List.of(), Map.of(), partial)
                : new Blocks(overview.groups(), listing.entries(), listing.unlisted(), partial);
        }
    }

    static final class FindBlocks extends AbstractTool<Search, Found> {

        private static final int MAX_SIDE = 129;

        private final Game game;
        private final LoadedBlocks blocks;
        private final Details details;

        FindBlocks(Game game, LoadedBlocks blocks, Details details) {
            super(
                "find_blocks",
                "Find blocks",
                "Every block whose name contains the query, one by one with position, inventory, fluids and neighbours.");
            this.game = game;
            this.blocks = blocks;
            this.details = details;
        }

        public Found call(Search search) {
            EntityPlayer player = game.player();
            Box box = Box.of(player, search.radius(), search.from(), search.to(), 32)
                .limit(MAX_SIDE);
            Listing listing = new Listing(search.query(), LISTED_UP_TO, details.pick(search.neighbours()));
            boolean partial = blocks.walk(player.worldObj, box, List.of(listing));
            return new Found(listing.entries(), listing.unlisted(), partial);
        }
    }

    static final class GetBlock extends AbstractTool<Position, Listing.Entry> {

        private final Game game;
        private final LoadedBlocks blocks;
        private final Details details;

        GetBlock(Game game, LoadedBlocks blocks, Details details) {
            super(
                "get_block",
                "Block at",
                "One block with inventory, fluids and neighbours; step through neighbours to follow a setup.");
            this.game = game;
            this.blocks = blocks;
            this.details = details;
        }

        public Listing.Entry call(Position position) {
            List<Integer> at = position.at();
            if (at == null || at.size() != 3) throw new ToolException("at is [x, y, z]");
            BlockAt block = blocks.at(game.player().worldObj, at.get(0), at.get(1), at.get(2));
            if (block == null) throw new ToolException("No block at " + at + ": air or not loaded");
            return Listing.Entry.of(block, blocks.name(block), details.pick(position.neighbours()));
        }
    }
}
