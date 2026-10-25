package dev.gtnhmcp.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.AbstractTool;
import dev.gtnhmcp.core.Game;

/** Reading blocks: an overview of an area with chosen blocks listed in full, or matching blocks across a larger one. */
final class BlockTools {

    private static final int LISTED_UP_TO = 100;

    private BlockTools() {}

    public record Area(@JsonPropertyDescription("blocks around the player, default 5, max 32") Integer radius,
        @JsonPropertyDescription("corner [x, y, z]; with to, used instead of radius") List<Integer> from,
        @JsonPropertyDescription("opposite corner [x, y, z]") List<Integer> to,
        @JsonPropertyDescription("text in block names to also list one by one with position and details, or '*' for all") String expand) {}

    public record Search(@JsonProperty(
        required = true) @JsonPropertyDescription("text in the block name, e.g. 'blast furnace', 'drawer'") String query,
        @JsonPropertyDescription("blocks around the player, default 32, max 64") Integer radius,
        @JsonPropertyDescription("corner [x, y, z]; with to, used instead of radius") List<Integer> from,
        @JsonPropertyDescription("opposite corner [x, y, z]") List<Integer> to) {}

    public record Blocks(@JsonPropertyDescription("by name, rarest first") Map<String, Overview.Group> blocks,
        @JsonPropertyDescription("the blocks matching expand, one by one") List<Listing.Entry> expanded,
        @JsonPropertyDescription("matching blocks past the listing limit") int unlisted,
        @JsonPropertyDescription("true when part of the area is in unloaded chunks and was skipped") boolean partial) {}

    public record Found(List<Listing.Entry> blocks,
        @JsonPropertyDescription("matching blocks past the listing limit") int unlisted,
        @JsonPropertyDescription("true when part of the area is in unloaded chunks and was skipped") boolean partial) {}

    static final class GetBlocks extends AbstractTool<Area, Blocks> {

        private static final int MAX_SIDE = 65;
        private static final int POSITIONS_UP_TO = 8;

        private final Game game;
        private final LoadedBlocks blocks;
        private final List<BlockDetail> details;

        GetBlocks(Game game, LoadedBlocks blocks, List<BlockDetail> details) {
            super(
                "get_blocks",
                "Blocks around",
                "Every non-air block in an area grouped by name (machines by machine); with expand, matching blocks "
                    + "also one by one with position, inventory and fluids. Use it to see what a production line is.");
            this.game = game;
            this.blocks = blocks;
            this.details = List.copyOf(details);
        }

        public Blocks call(Area area) {
            EntityPlayer player = game.player();
            Box box = Box.of(player, area.radius(), area.from(), area.to(), 5)
                .limit(MAX_SIDE);
            Overview overview = new Overview(POSITIONS_UP_TO);
            List<BlockSink> sinks = new ArrayList<>(List.of(overview));
            Listing listing = area.expand() == null ? null : new Listing(area.expand(), LISTED_UP_TO, details);
            if (listing != null) sinks.add(listing);
            boolean partial = blocks.walk(player.worldObj, box, sinks);
            return listing == null ? new Blocks(overview.groups(), List.of(), 0, partial)
                : new Blocks(overview.groups(), listing.entries(), listing.unlisted(), partial);
        }
    }

    static final class FindBlocks extends AbstractTool<Search, Found> {

        private static final int MAX_SIDE = 129;

        private final Game game;
        private final LoadedBlocks blocks;
        private final List<BlockDetail> details;

        FindBlocks(Game game, LoadedBlocks blocks, List<BlockDetail> details) {
            super(
                "find_blocks",
                "Find blocks",
                "Every block whose name contains the query, one by one with position, inventory and fluids.");
            this.game = game;
            this.blocks = blocks;
            this.details = List.copyOf(details);
        }

        public Found call(Search search) {
            EntityPlayer player = game.player();
            Box box = Box.of(player, search.radius(), search.from(), search.to(), 32)
                .limit(MAX_SIDE);
            Listing listing = new Listing(search.query(), LISTED_UP_TO, details);
            boolean partial = blocks.walk(player.worldObj, box, List.of(listing));
            return new Found(listing.entries(), listing.unlisted(), partial);
        }
    }
}
