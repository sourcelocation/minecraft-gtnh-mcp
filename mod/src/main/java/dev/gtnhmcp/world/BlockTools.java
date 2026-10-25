package dev.gtnhmcp.world;

import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.AbstractTool;
import dev.gtnhmcp.core.Game;

/** Reading blocks: everything in a small area, or matching blocks in a larger one. Only loaded chunks are seen. */
final class BlockTools {

    private BlockTools() {}

    public record Area(@JsonPropertyDescription("blocks around the player, default 5, max 32") Integer radius,
        @JsonPropertyDescription("corner [x, y, z]; with to, used instead of radius") List<Integer> from,
        @JsonPropertyDescription("opposite corner [x, y, z]") List<Integer> to) {}

    public record Search(@JsonProperty(
        required = true) @JsonPropertyDescription("text in the block name, e.g. 'blast furnace', 'drawer', 'running'") String query,
        @JsonPropertyDescription("blocks around the player, default 32, max 64") Integer radius,
        @JsonPropertyDescription("corner [x, y, z]; with to, used instead of radius") List<Integer> from,
        @JsonPropertyDescription("opposite corner [x, y, z]") List<Integer> to) {}

    static final class GetBlocks extends AbstractTool<Area, BlockScanner.Scan> {

        private static final int MAX_SIDE = 65;
        private static final int LIST_UP_TO = 8;

        private final Game game;
        private final BlockScanner scanner;

        GetBlocks(Game game, BlockScanner scanner) {
            super(
                "get_blocks",
                "Blocks around",
                "Every non-air block in an area, grouped by name: GregTech machines by machine (with '(running)'), "
                    + "drawers by contents. Use it to see what a production line is.");
            this.game = game;
            this.scanner = scanner;
        }

        public BlockScanner.Scan call(Area area) {
            EntityPlayer player = game.player();
            Box box = Box.of(player, area.radius(), area.from(), area.to(), 5)
                .limit(MAX_SIDE);
            return scanner.scan(player.worldObj, box, name -> true, LIST_UP_TO);
        }
    }

    static final class FindBlocks extends AbstractTool<Search, BlockScanner.Scan> {

        private static final int MAX_SIDE = 129;
        private static final int LIST_UP_TO = 32;

        private final Game game;
        private final BlockScanner scanner;

        FindBlocks(Game game, BlockScanner scanner) {
            super(
                "find_blocks",
                "Find blocks",
                "Positions of blocks whose name contains the query, e.g. every Electric Blast Furnace nearby.");
            this.game = game;
            this.scanner = scanner;
        }

        public BlockScanner.Scan call(Search search) {
            EntityPlayer player = game.player();
            Box box = Box.of(player, search.radius(), search.from(), search.to(), 32)
                .limit(MAX_SIDE);
            String query = search.query()
                .toLowerCase(Locale.ROOT);
            return scanner.scan(
                player.worldObj,
                box,
                name -> name.toLowerCase(Locale.ROOT)
                    .contains(query),
                LIST_UP_TO);
        }
    }
}
