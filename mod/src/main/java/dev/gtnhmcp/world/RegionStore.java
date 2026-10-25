package dev.gtnhmcp.world;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import cpw.mods.fml.common.Loader;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Boxes with a note the player or the model put on the world, kept in {@code config/gtnhmcp/regions/<world>.json}.
 * They are this mod's notes, not game state: the world itself is never changed.
 */
final class RegionStore {

    record Region(String note, Box box) {}

    /** The file format: a region with its box's corners as arrays. */
    private record Stored(String note, int dim, int[] min, int[] max) {

        static Stored of(Region region) {
            Box box = region.box();
            return new Stored(
                region.note(),
                box.dim(),
                new int[] { box.minX(), box.minY(), box.minZ() },
                new int[] { box.maxX(), box.maxY(), box.maxZ() });
        }

        Region region() {
            return new Region(note, new Box(dim, min[0], min[1], min[2], max[0], max[1], max[2]));
        }
    }

    private static final TypeReference<List<Stored>> LIST = new TypeReference<>() {};

    private final JsonMapper json = JsonMapper.builder()
        .build();
    private final File dir = new File(
        Loader.instance()
            .getConfigDir(),
        "gtnhmcp/regions");

    List<Region> all() {
        File file = file();
        if (!file.isFile()) return List.of();
        try {
            return json.readValue(file, LIST)
                .stream()
                .map(Stored::region)
                .toList();
        } catch (JacksonException e) {
            throw new IllegalStateException("Unreadable region file " + file, e);
        }
    }

    /** Adds the region, replacing one with the same note. */
    void put(Region region) {
        List<Region> regions = new ArrayList<>(all());
        regions.removeIf(
            r -> r.note()
                .equalsIgnoreCase(region.note()));
        regions.add(region);
        save(regions);
    }

    /** Returns false when no region had that note. */
    boolean remove(String note) {
        List<Region> regions = new ArrayList<>(all());
        boolean removed = regions.removeIf(
            r -> r.note()
                .equalsIgnoreCase(note));
        if (removed) save(regions);
        return removed;
    }

    private void save(List<Region> regions) {
        dir.mkdirs();
        json.writeValue(
            file(),
            regions.stream()
                .map(Stored::of)
                .toList());
    }

    /** One file per singleplayer save or multiplayer server. */
    private File file() {
        Minecraft mc = Minecraft.getMinecraft();
        String world;
        if (mc.isSingleplayer() && mc.getIntegratedServer() != null) world = "sp_" + mc.getIntegratedServer()
            .getFolderName();
        else {
            ServerData server = mc.func_147104_D();
            world = "mp_" + (server == null ? "unknown" : server.serverIP);
        }
        return new File(dir, world.replaceAll("[^A-Za-z0-9._-]", "_") + ".json");
    }
}
