package de.waterdu.atlantis.level.schematic;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Loads schematics, places them by id and keeps the history of placements.
 */
public final class Schematics {

    private static final Map<String, Schematic> LOADED_SCHEMATICS = new ConcurrentHashMap<>();
    private static final Deque<SchematicHistory> SCHEMATIC_HISTORY = new ConcurrentLinkedDeque<>();

    private Schematics() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Loads every file in the {@code atlantis/schematics} directory of the config directory with {@link #load(File)}.
     */
    public static void loadAll() {
        var dir = Mods.modLoader().configDir().resolve(Atlantis.ID).resolve("schematics").toFile();
        if (dir.mkdirs()) {
            AtlantisLogger.info("Created schematics directory at {}", dir.getPath());
        }
        var files = dir.listFiles();
        if (files == null) {
            files = new File[0];
        }
        AtlantisLogger.info("Attempting to load {} schematic{}", files.length, files.length == 1 ? "" : "s");
        for (var file : files) {
            Schematics.load(file);
        }
    }

    /**
     * Loads a compressed NBT schematic on the Atlantis thread pool, so it is not available when this returns.
     * The schematic id is the file name without its extension.
     */
    public static void load(File file) {
        Atlantis.THREAD_POOL.submit(() -> {
            try {
                var name = file.getName().substring(0, file.getName().lastIndexOf("."));
                LOADED_SCHEMATICS.put(name, new Schematic(name, NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap())));
                AtlantisLogger.info("Loaded schematic from file {}", file.getName());
            } catch (IOException e) {
                AtlantisLogger.error("Failed to load schematic from file {}, exception: {}", file.getName(), e);
            }
        });
    }

    /**
     * @return the ids of the loaded schematics
     */
    public static Set<String> ids() {
        return LOADED_SCHEMATICS.keySet();
    }

    /**
     * @return null when no schematic with this id is loaded
     */
    public static Schematic get(String id) {
        return LOADED_SCHEMATICS.get(id);
    }

    /**
     * {@link #place(String, ServerLevel, BlockPos)} at the coordinates, cast to integers.
     *
     * @param id the id of the schematic
     * @param world the level to place the schematic in
     * @param x the x coordinate of the lowest corner
     * @param y the y coordinate of the lowest corner
     * @param z the z coordinate of the lowest corner
     * @return {@code false} when no schematic with this id is loaded
     */
    public static boolean place(String id, ServerLevel world, double x, double y, double z) {
        return Schematics.place(id, world, new BlockPos((int) x, (int) y, (int) z));
    }

    /**
     * {@link #place(String, ServerLevel, BlockPos, int, boolean)} at the coordinates, cast to integers.
     *
     * @param id the id of the schematic
     * @param world the level to place the schematic in
     * @param x the x coordinate of the lowest corner
     * @param y the y coordinate of the lowest corner
     * @param z the z coordinate of the lowest corner
     * @param passes the number of times to place all blocks
     * @param ignoreAir does not place the air blocks of the schematic
     * @return {@code false} when no schematic with this id is loaded
     */
    public static boolean place(String id, ServerLevel world, double x, double y, double z, int passes, boolean ignoreAir) {
        return Schematics.place(id, world, new BlockPos((int) x, (int) y, (int) z), passes, ignoreAir);
    }

    /**
     * {@link #place(String, ServerLevel, BlockPos, int, boolean)} with one pass that skips air.
     *
     * @param id the id of the schematic
     * @param world the level to place the schematic in
     * @param pos the lowest corner of the schematic
     * @return {@code false} when no schematic with this id is loaded
     */
    public static boolean place(String id, ServerLevel world, BlockPos pos) {
        return Schematics.place(id, world, pos, 1, true);
    }

    /**
     * {@link Schematic#place(ServerLevel, BlockPos, int, boolean)} for the schematic with this id.
     *
     * @return {@code false} when no schematic with this id is loaded
     */
    public static boolean place(String id, ServerLevel world, BlockPos pos, int passes, boolean ignoreAir) {
        var schematic = Schematics.get(id);
        if (schematic == null) {
            return false;
        }
        schematic.place(world, pos, passes, ignoreAir);
        return true;
    }

    /**
     * Adds the history as the newest entry. Removes the oldest entries when there are more than the configured history depth.
     */
    public static void offerHistory(SchematicHistory history) {
        SCHEMATIC_HISTORY.offer(history);
        while (SCHEMATIC_HISTORY.size() > Settings.get().getSchematicHistoryDepth()) {
            SCHEMATIC_HISTORY.pop();
        }
    }

    /**
     * @return the newest history, which is removed from the list. null when the list is empty.
     */
    public static SchematicHistory pollHistory() {
        return SCHEMATIC_HISTORY.pollLast();
    }
}
