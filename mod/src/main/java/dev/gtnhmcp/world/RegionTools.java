package dev.gtnhmcp.world;

import java.util.Comparator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import dev.gtnhmcp.core.AbstractTool;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.ToolException;

/** Noted regions: what production lines and areas are for, kept per world by this mod. */
final class RegionTools {

    private RegionTools() {}

    public record Label(@JsonProperty(
        required = true) @JsonPropertyDescription("what it is and how it works, e.g. 'PE line — ethylene + O2 → polyethylene'; replaces a region with the same note") String note,
        @JsonPropertyDescription("blocks around the player, default 5") Integer radius,
        @JsonPropertyDescription("corner [x, y, z]; with to, used instead of radius") List<Integer> from,
        @JsonPropertyDescription("opposite corner [x, y, z]") List<Integer> to) {}

    public record Note(@JsonProperty(required = true) String note) {}

    public record Nearby(@JsonPropertyDescription("blocks from the player, default 200") Integer radius,
        @JsonPropertyDescription("corner [x, y, z]; with to, regions overlapping this box instead") List<Integer> from,
        @JsonPropertyDescription("opposite corner [x, y, z]") List<Integer> to) {}

    public record RegionView(String note, int dim, @JsonPropertyDescription("x1,y1,z1..x2,y2,z2") String area,
        @JsonPropertyDescription("blocks from the player, in the same dimension") Integer distance) {}

    public record Regions(List<RegionView> regions) {}

    static RegionView view(RegionStore.Region region, EntityPlayer player) {
        return new RegionView(
            region.note(),
            region.box()
                .dim(),
            region.box()
                .text(),
            region.box()
                .distanceTo(player));
    }

    static final class LabelRegion extends AbstractTool<Label, RegionView> {

        private final Game game;
        private final RegionStore regions;

        LabelRegion(Game game, RegionStore regions) {
            super(
                "label_region",
                "Label region",
                "Note what an area is, e.g. a production line and how it works, so later sessions know it.");
            this.game = game;
            this.regions = regions;
        }

        public Access access() {
            return Access.WRITE;
        }

        public RegionView call(Label label) {
            if (label.note()
                .isBlank()) throw new ToolException("note required");
            EntityPlayer player = game.player();
            RegionStore.Region region = new RegionStore.Region(
                label.note()
                    .trim(),
                Box.of(player, label.radius(), label.from(), label.to(), 5));
            regions.put(region);
            return view(region, player);
        }
    }

    public record Deleted(String deleted) {}

    static final class DeleteRegion extends AbstractTool<Note, Deleted> {

        private final RegionStore regions;

        DeleteRegion(RegionStore regions) {
            super("delete_region", "Delete region", "Remove a region by its note.");
            this.regions = regions;
        }

        public Access access() {
            return Access.DELETE;
        }

        public Deleted call(Note note) {
            if (!regions.remove(note.note())) throw new ToolException("No region noted '" + note.note() + "'");
            return new Deleted(note.note());
        }
    }

    static final class ListRegions extends AbstractTool<Nearby, Regions> {

        private final Game game;
        private final RegionStore regions;

        ListRegions(Game game, RegionStore regions) {
            super("list_regions", "Regions", "Noted regions near the player, or overlapping an area, nearest first.");
            this.game = game;
            this.regions = regions;
        }

        public Regions call(Nearby nearby) {
            EntityPlayer player = game.player();
            Box area = Box.of(player, nearby.radius(), nearby.from(), nearby.to(), 200);
            return new Regions(
                regions.all()
                    .stream()
                    .filter(
                        region -> region.box()
                            .intersects(area))
                    .map(region -> view(region, player))
                    .sorted(Comparator.comparing(view -> view.distance() == null ? Integer.MAX_VALUE : view.distance()))
                    .toList());
        }
    }
}
