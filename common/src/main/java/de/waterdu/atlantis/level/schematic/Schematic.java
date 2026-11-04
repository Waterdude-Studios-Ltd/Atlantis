package de.waterdu.atlantis.level.schematic;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.java.collection.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

// Created with inspiration from WorldEdit's source code, which is licenced under GPLv3.
// The license text for WorldEdit can be found at https://github.com/EngineHub/WorldEdit/blob/version/7.3.x/LICENSE.txt
// WorldEdit's repository: https://github.com/EngineHub/WorldEdit/

/**
 * Holds a Sponge schematic that is read from NBT, and places its blocks in a level.
 */
public class Schematic {

    private static final String PALETTE_MAX_TAG = "PaletteMax";
    private static final String PALETTE_TAG = "Palette";
    private static final String VERSION_TAG = "Version";
    private static final String LENGTH_TAG = "Length";
    private static final String HEIGHT_TAG = "Height";
    private static final String WIDTH_TAG = "Width";
    private static final String METADATA_TAG = "Metadata";
    private static final String DATA_VERSION_TAG = "DataVersion";
    private static final String BLOCK_DATA_TAG = "BlockData";
    private static final String BLOCK_ENTITIES_TAG = "BlockEntities";
    private static final String OFFSETS_TAG = "Offset";

    private final String name;

    private final int paletteMax;
    private final Map<Integer, SchematicBlock> palette = new Int2ObjectOpenHashMap<>();

    private byte[] blockData;
    private final Map<BlockPos, SchematicBlockEntity> blockEntities = new Object2ObjectOpenHashMap<>();

    private final Vector3i dimensions;
    private final Vector3i offsets;

    private final int version;
    private final int dataVersion;
    private CompoundTag metadata;

    private final Map<BlockPos, Pair<SchematicBlock, SchematicBlockEntity>> preparedSchematic = new LinkedHashMap<>();

    /**
     * Reads the palette, block data, block entities, dimensions, offsets and metadata from the tag.
     *
     * @param name the id of the schematic
     * @param schematicTag the root tag of the schematic file
     */
    protected Schematic(String name, CompoundTag schematicTag) {
        this.name = name;
        this.paletteMax = schematicTag.getInt(PALETTE_MAX_TAG);
        var paletteTag = schematicTag.getCompound(PALETTE_TAG);
        for (var paletteKey : paletteTag.getAllKeys()) {
            this.palette.put(paletteTag.getInt(paletteKey), new SchematicBlock(name, paletteKey));
        }
        this.blockData = schematicTag.getByteArray(BLOCK_DATA_TAG);
        for (var blockEntityTag : schematicTag.getList(BLOCK_ENTITIES_TAG, Tag.TAG_COMPOUND)) {
            var blockEntity = SchematicBlockEntity.fromTag(name, (CompoundTag) blockEntityTag);
            if (blockEntity != null) {
                this.blockEntities.put(blockEntity.position(), blockEntity);
            }
        }
        this.dimensions = new Vector3i(schematicTag.getShort(WIDTH_TAG), schematicTag.getShort(HEIGHT_TAG), schematicTag.getShort(LENGTH_TAG));
        var offsetsData = schematicTag.getIntArray(OFFSETS_TAG);
        if (offsetsData.length == 3) {
            this.offsets = new Vector3i(offsetsData[0], offsetsData[1], offsetsData[2]);
        } else {
            this.offsets = new Vector3i(0, 0, 0);
        }
        this.version = schematicTag.getInt(VERSION_TAG);
        this.dataVersion = schematicTag.getInt(DATA_VERSION_TAG);
        this.metadata = schematicTag.getCompound(METADATA_TAG);
    }

    /**
     * @return the id of the schematic
     */
    public String name() {
        return this.name;
    }

    /**
     * Decodes the block data into the list of blocks to place. The place methods call this when needed.
     * Throws {@link IllegalArgumentException} when the block data is corrupt.
     */
    public void prepare() throws IllegalArgumentException {
        var index = 0;
        var i = 0;
        int value;
        int intLen;

        var width = this.dimensions.x();
        var length = this.dimensions.z();

        while (i < this.blockData.length) {
            value = 0;
            intLen = 0;

            while (true) {
                value |= (this.blockData[i] & 127) << (intLen++ * 7);
                if (intLen > 5) {
                    throw new IllegalArgumentException("Corrupt schematic: " + this.name);
                }
                if ((this.blockData[i] & 128) != 128) {
                    i++;
                    break;
                }
                i++;
            }
            var y = index / (width * length);
            var z = (index % (width * length)) / width;
            var x = (index % (width * length)) % width;
            var state = this.palette.get(value);
            var pos = new BlockPos(x, y, z);
            this.preparedSchematic.put(pos, Pair.of(state, this.blockEntities.get(pos)));
            index++;
        }
    }

    /**
     * Removes the raw schematic data to free memory. Call this only after {@link #prepare()},
     * because {@link #prepare()} cannot decode the blocks after it.
     */
    public void clean() {
        this.palette.clear();
        this.blockEntities.clear();
        this.blockData = new byte[0];
        this.metadata = null;
    }

    /**
     * {@link #place(ServerLevel, BlockPos, int, boolean)} with one pass that skips air.
     */
    public void place(ServerLevel world, BlockPos origin) {
        this.place(world, origin, 1, true);
    }

    /**
     * Places every block now and adds the replaced blocks to the history of {@link Schematics}.
     *
     * @param origin the lowest corner of the schematic. The offset in the schematic file is not used.
     * @param passes the number of times to place all blocks. More passes help blocks that need support from other
     *     blocks.
     * @param ignoreAir does not place the air blocks of the schematic, so the blocks at those positions stay
     */
    public void place(ServerLevel world, BlockPos origin, int passes, boolean ignoreAir) {
        this.tryPrepare();
        var history = new SchematicHistory(world);
        for (var i = 0; i < passes; i++) {
            this.placeInner(world, origin, ignoreAir, history);
        }
        Schematics.offerHistory(history);
    }

    /**
     * Places the blocks in steps on the server thread. Each step places {@code blocksPerStep} blocks.
     *
     * @param origin the lowest corner of the schematic. The offset in the schematic file is not used.
     * @param ignoreAir does not place the air blocks of the schematic, so the blocks at those positions stay
     * @param ticksPerStep the delay between steps, in ticks
     * @return a future that completes on the server thread after the last block is placed. The history is also added to
     *     {@link Schematics}.
     */
    public CompletableFuture<SchematicHistory> placeAsync(ServerLevel level, BlockPos origin, boolean ignoreAir, int blocksPerStep, int ticksPerStep) {
        var future = new CompletableFuture<SchematicHistory>();
        var history = new SchematicHistory(level);
        this.tryPrepare();
        var millisPerStep = 50 * ticksPerStep;
        Concurrency.async(() -> {
            var queue = new ConcurrentLinkedQueue<>(this.preparedSchematic.entrySet());
            Atlantis.THREAD_POOL.repeatUntilComplete(() -> {
                var toPlace = new ArrayList<Map.Entry<BlockPos, Pair<SchematicBlock, SchematicBlockEntity>>>();
                for (var i = 0; i < blocksPerStep; i++) {
                    var entry = queue.poll();
                    if (entry != null) {
                        toPlace.add(entry);
                    }
                }
                var done = queue.isEmpty();
                Concurrency.sync(() -> {
                    for (var entry : toPlace) {
                        this.placeMapEntry(level, origin, entry, ignoreAir, history);
                    }
                    if (done) {
                        Schematics.offerHistory(history);
                        future.complete(history);
                    }
                });
                return done;
            }, millisPerStep, TimeUnit.MILLISECONDS);
        });
        return future;
    }

    /**
     * Places every prepared block once.
     *
     * @param world the level to place the blocks in
     * @param origin the lowest corner of the schematic
     * @param ignoreAir does not place the air blocks of the schematic
     * @param history the history that stores the replaced blocks
     */
    private void placeInner(ServerLevel world, BlockPos origin, boolean ignoreAir, SchematicHistory history) {
        for (var entry : this.preparedSchematic.entrySet()) {
            this.placeMapEntry(world, origin, entry, ignoreAir, history);
        }
    }

    /**
     * Places one prepared block relative to the origin. Logs a warning and places nothing if the entry, its block or
     * the origin is {@code null}.
     *
     * @param world the level to place the block in
     * @param origin the lowest corner of the schematic
     * @param entry the relative position, and the block with its block entity
     * @param ignoreAir does not place the block if it is air
     * @param history the history that stores the replaced block
     */
    private void placeMapEntry(ServerLevel world, BlockPos origin,
                               Map.Entry<BlockPos, Pair<SchematicBlock, SchematicBlockEntity>> entry,
                               boolean ignoreAir, SchematicHistory history) {
        if (entry == null) {
            AtlantisLogger.warn("Null schematic map entry");
            return;
        }

        var value = entry.getValue();

        if (value == null) {
            AtlantisLogger.warn("Null schematic map entry value");
            return;
        }

        var block = value.getA();

        if (block == null) {
            AtlantisLogger.warn("Null schematic block");
            return;
        }

        if (origin == null) {
            AtlantisLogger.warn("Null schematic origin");
            return;
        }

        block.place(world, origin.offset(entry.getKey()), value.getB(), ignoreAir, history);
    }

    /**
     * Calls {@link #prepare()} if no blocks are prepared yet, then {@link #clean()} if the settings enable it. Logs an
     * error if the block data is corrupt.
     */
    private void tryPrepare() {
        if (this.preparedSchematic.isEmpty()) {
            try {
                this.prepare();
                if (Settings.get().isCleanSchematicsAfterPrepare()) {
                    this.clean();
                }
            } catch (IllegalArgumentException e) {
                AtlantisLogger.error("Failed to prepare schematic: " + this.name);
            }
        }
    }
}
