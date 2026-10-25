package dev.gtnhmcp.vein;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.sinthoras.visualprospecting.database.ClientCache;
import com.sinthoras.visualprospecting.database.OreVeinPosition;

import dev.gtnhmcp.core.AbstractTool;
import dev.gtnhmcp.core.Game;
import dev.gtnhmcp.core.Page;
import dev.gtnhmcp.core.Text;
import dev.gtnhmcp.core.ToolException;

/** The ore veins the player has prospected, as VisualProspecting remembers them, nearest first. */
final class FindVeins extends AbstractTool<FindVeins.Search, Page<FindVeins.Vein>> {

    public record Search(@JsonPropertyDescription("text in the vein or ore names, e.g. 'bauxite'") String query,
        @JsonPropertyDescription("only this dimension id; 0 is the Overworld") Integer dim, String cursor,
        Integer limit) {}

    public record Vein(String name, List<String> ores, int dim, int x, int z,
        @JsonPropertyDescription("blocks from the player; only in the player's dimension") Integer distance,
        boolean depleted) {}

    private final Game game;

    FindVeins(Game game) {
        super(
            "find_veins",
            "Find ore veins",
            "Ore veins the player has prospected (VisualProspecting), nearest first in the current dimension.");
        this.game = game;
    }

    public Page<Vein> call(Search search) {
        EntityPlayer player = playerOrNull();
        String query = search.query() == null ? null
            : search.query()
                .toLowerCase(Locale.ROOT);
        List<Vein> veins = ClientCache.instance.getAllOreVeins()
            .stream()
            .filter(vein -> search.dim() == null || vein.dimensionId == search.dim())
            .map(vein -> vein(vein, player))
            .filter(vein -> query == null || matches(vein, query))
            .sorted(nearestFirst(player))
            .toList();
        return Page.of(veins.stream(), search.cursor(), search.limit());
    }

    private static Vein vein(OreVeinPosition position, EntityPlayer player) {
        Integer distance = player == null || player.dimension != position.dimensionId ? null
            : (int) Math.hypot(position.getBlockX() - player.posX, position.getBlockZ() - player.posZ);
        return new Vein(
            Text.clean(position.veinType.getVeinName()),
            position.veinType.getOreMaterialNames()
                .stream()
                .map(Text::clean)
                .toList(),
            position.dimensionId,
            position.getBlockX(),
            position.getBlockZ(),
            distance,
            position.isDepleted());
    }

    private static boolean matches(Vein vein, String query) {
        return vein.name()
            .toLowerCase(Locale.ROOT)
            .contains(query)
            || vein.ores()
                .stream()
                .anyMatch(
                    ore -> ore.toLowerCase(Locale.ROOT)
                        .contains(query));
    }

    /** The player's dimension first, nearest first; then other dimensions in id order. */
    private static Comparator<Vein> nearestFirst(EntityPlayer player) {
        int here = player == null ? Integer.MIN_VALUE : player.dimension;
        return Comparator.comparing((Vein vein) -> vein.dim() != here)
            .thenComparingInt(Vein::dim)
            .thenComparingInt(vein -> vein.distance() == null ? Integer.MAX_VALUE : vein.distance());
    }

    private EntityPlayer playerOrNull() {
        try {
            return game.player();
        } catch (ToolException notInWorld) {
            return null;
        }
    }
}
