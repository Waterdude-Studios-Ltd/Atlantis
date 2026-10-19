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

public final class Schematics {

    private static final Map<String, Schematic> LOADED_SCHEMATICS = new ConcurrentHashMap<>();
    private static final Deque<SchematicHistory> SCHEMATIC_HISTORY = new ConcurrentLinkedDeque<>();

    private Schematics() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

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

    public static Set<String> ids() {
        return LOADED_SCHEMATICS.keySet();
    }

    public static Schematic get(String id) {
        return LOADED_SCHEMATICS.get(id);
    }

    public static boolean place(String id, ServerLevel world, double x, double y, double z) {
        return Schematics.place(id, world, new BlockPos((int) x, (int) y, (int) z));
    }

    public static boolean place(String id, ServerLevel world, double x, double y, double z, int passes, boolean ignoreAir) {
        return Schematics.place(id, world, new BlockPos((int) x, (int) y, (int) z), passes, ignoreAir);
    }

    public static boolean place(String id, ServerLevel world, BlockPos pos) {
        return Schematics.place(id, world, pos, 1, true);
    }

    public static boolean place(String id, ServerLevel world, BlockPos pos, int passes, boolean ignoreAir) {
        var schematic = Schematics.get(id);
        if (schematic == null) {
            return false;
        }
        schematic.place(world, pos, passes, ignoreAir);
        return true;
    }

    public static void offerHistory(SchematicHistory history) {
        SCHEMATIC_HISTORY.offer(history);
        while (SCHEMATIC_HISTORY.size() > Settings.get().getSchematicHistoryDepth()) {
            SCHEMATIC_HISTORY.pop();
        }
    }

    public static SchematicHistory pollHistory() {
        return SCHEMATIC_HISTORY.pollLast();
    }
}
