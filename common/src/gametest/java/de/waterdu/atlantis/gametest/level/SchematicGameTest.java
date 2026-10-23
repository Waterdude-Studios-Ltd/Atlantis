package de.waterdu.atlantis.gametest.level;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.level.schematic.Schematic;
import de.waterdu.atlantis.level.schematic.SchematicHistory;
import de.waterdu.atlantis.level.schematic.Schematics;
import de.waterdu.atlantis.mod.Mods;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Schematic loading, placing and undoing. The fixture is written when the tests are first run:
 * <pre>
 *   y=1:  air    stone
 *   y=0:  stone  chest (5 diamonds in slot 0)
 *         x=0    x=1
 * </pre>
 * The schematic history is global, so tests that undo run in batches where nothing else places a schematic.
 */
public final class SchematicGameTest {

    private static final String BATCH = "atlantis_schematics";
    private static final String ASYNC_BATCH = "atlantis_schematics_async";
    private static final String COMMAND_BATCH = "atlantis_schematics_command";
    private static final String NAME = "atlantis_test_schematic";
    private static final int TIMEOUT = 100000;

    private static final AtomicBoolean WRITTEN = new AtomicBoolean();

    private SchematicGameTest() {}

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void loadListsTheSchematic(GameTestHelper helper) {
        helper.succeedWhen(() -> {
            loaded(helper);
            helper.assertTrue(Schematics.ids().contains(NAME), "schematic not listed");
            helper.assertValueEqual(Schematics.get(NAME).name(), NAME, "name");
        });
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void placeSetsBlocksAndBlockEntitiesAndSkipsAir(GameTestHelper helper) {
        var origin = new BlockPos(1, 2, 1);
        helper.setBlock(origin.offset(0, 1, 0), Blocks.GLASS);

        helper.startSequence()
                .thenWaitUntil(() -> loaded(helper))
                .thenExecute(() -> {
                    helper.assertTrue(Schematics.place(NAME, helper.getLevel(), helper.absolutePos(origin), 1, true), "place");
                    assertPlaced(helper, origin);
                    helper.assertBlockPresent(Blocks.GLASS, origin.offset(0, 1, 0));
                })
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void placeWithAirReplacesBlocks(GameTestHelper helper) {
        var origin = new BlockPos(1, 2, 1);
        helper.setBlock(origin.offset(0, 1, 0), Blocks.GLASS);

        helper.startSequence()
                .thenWaitUntil(() -> loaded(helper))
                .thenExecute(() -> {
                    helper.assertTrue(Schematics.place(NAME, helper.getLevel(), helper.absolutePos(origin), 1, false), "place");
                    assertPlaced(helper, origin);
                    helper.assertBlockPresent(Blocks.AIR, origin.offset(0, 1, 0));
                })
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void placingAnUnknownSchematicFails(GameTestHelper helper) {
        helper.assertFalse(Schematics.place("atlantis_test_missing", helper.getLevel(), helper.absolutePos(BlockPos.ZERO)), "placed a missing schematic");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void undoRestoresTheBlocksFromBeforeEveryPass(GameTestHelper helper) {
        var origin = new BlockPos(1, 2, 1);
        fill(helper, origin, Blocks.DIRT);

        helper.startSequence()
                .thenWaitUntil(() -> loaded(helper))
                .thenExecute(() -> {
                    helper.assertTrue(Schematics.place(NAME, helper.getLevel(), helper.absolutePos(origin), 2, false), "place");
                    assertPlaced(helper, origin);
                    var history = Schematics.pollHistory();
                    helper.assertTrue(history != null, "no history");
                    history.restore();
                    assertFilled(helper, origin, Blocks.DIRT);
                    helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(origin.offset(1, 0, 0))) == null, "the chest's block entity was kept");
                })
                .thenSucceed();
    }

    @AtlantisGameTest(batch = ASYNC_BATCH, timeoutTicks = TIMEOUT)
    public static void placeAsyncPlacesEveryBlockBeforeCompleting(GameTestHelper helper) {
        var origin = new BlockPos(1, 2, 1);
        var completed = new AtomicReference<SchematicHistory>();
        var placedOnCompletion = new AtomicReference<RuntimeException>();

        helper.startSequence()
                .thenWaitUntil(() -> loaded(helper))
                .thenExecute(() -> schematic().placeAsync(helper.getLevel(), helper.absolutePos(origin), false, 1, 1).thenAccept(history -> {
                    try {
                        assertPlaced(helper, origin);
                    } catch (RuntimeException e) {
                        placedOnCompletion.set(e);
                    }
                    completed.set(history);
                }))
                .thenWaitUntil(() -> helper.assertTrue(completed.get() != null, "placeAsync did not complete"))
                .thenExecute(() -> {
                    if (placedOnCompletion.get() != null) {
                        throw new GameTestAssertException("not every block was placed on completion: " + placedOnCompletion.get().getMessage());
                    }
                    helper.assertTrue(Schematics.pollHistory() == completed.get(), "the async placement's history was not offered");
                    completed.get().restore();
                    assertFilled(helper, origin, Blocks.AIR);
                })
                .thenSucceed();
    }

    @AtlantisGameTest(batch = COMMAND_BATCH, timeoutTicks = TIMEOUT)
    public static void placeAndUndoCommands(GameTestHelper helper) {
        var start = helper.absolutePos(new BlockPos(1, 4, 1));
        var origin = new BlockPos(1, 2, 1);

        helper.startSequence()
                .thenWaitUntil(() -> loaded(helper))
                .thenExecute(() -> execute(helper, "atlantis schematics place " + NAME + " " + start.getX() + " " + start.getY() + " " + start.getZ() + " floor minecraft:overworld"))
                .thenWaitUntil(() -> assertPlaced(helper, origin))
                .thenExecute(() -> helper.assertValueEqual(execute(helper, "atlantis schematics undo"), 1, "undo result"))
                .thenExecute(() -> assertFilled(helper, origin, Blocks.AIR))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = COMMAND_BATCH, timeoutTicks = TIMEOUT)
    public static void reloadCommandLoadsTheSchematics(GameTestHelper helper) {
        helper.startSequence()
                .thenWaitUntil(() -> loaded(helper))
                .thenExecute(() -> execute(helper, "atlantis schematics reload"))
                .thenWaitUntil(() -> helper.assertTrue(Schematics.ids().contains(NAME), "schematic not listed after a reload"))
                .thenSucceed();
    }

    private static void loaded(GameTestHelper helper) {
        if (WRITTEN.compareAndSet(false, true)) {
            var dir = Mods.modLoader().configDir().resolve(Atlantis.ID).resolve("schematics");
            var file = dir.resolve(NAME + ".schem");
            try {
                Files.createDirectories(dir);
                NbtIo.writeCompressed(fixture(), file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            Schematics.load(file.toFile());
        }
        helper.assertTrue(Schematics.get(NAME) != null, "schematic not loaded yet");
    }

    private static Schematic schematic() {
        return Schematics.get(NAME);
    }

    private static CompoundTag fixture() {
        var tag = new CompoundTag();
        tag.putInt("Version", 2);
        tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        tag.putShort("Width", (short) 2);
        tag.putShort("Height", (short) 2);
        tag.putShort("Length", (short) 1);
        var palette = new CompoundTag();
        palette.putInt("minecraft:air", 0);
        palette.putInt("minecraft:stone", 1);
        palette.putInt("minecraft:chest[facing=north,type=single,waterlogged=false]", 2);
        tag.put("Palette", palette);
        tag.putInt("PaletteMax", 3);
        tag.putByteArray("BlockData", new byte[] {1, 2, 0, 1});
        var chest = new CompoundTag();
        chest.putString("Id", "minecraft:chest");
        chest.put("Pos", new IntArrayTag(new int[] {1, 0, 0}));
        var items = new ListTag();
        var diamonds = new CompoundTag();
        diamonds.putByte("Slot", (byte) 0);
        diamonds.putString("id", "minecraft:diamond");
        diamonds.putInt("count", 5);
        items.add(diamonds);
        chest.put("Items", items);
        var blockEntities = new ListTag();
        blockEntities.add(chest);
        tag.put("BlockEntities", blockEntities);
        return tag;
    }

    private static void assertPlaced(GameTestHelper helper, BlockPos origin) {
        helper.assertBlockPresent(Blocks.STONE, origin);
        helper.assertBlockPresent(Blocks.CHEST, origin.offset(1, 0, 0));
        helper.assertBlockPresent(Blocks.STONE, origin.offset(1, 1, 0));
        if (!(helper.getBlockEntity(origin.offset(1, 0, 0)) instanceof Container chest)) {
            throw new GameTestAssertException("the chest has no block entity");
        }
        helper.assertTrue(chest.getItem(0).is(Items.DIAMOND), "the chest's item is " + chest.getItem(0));
        helper.assertValueEqual(chest.getItem(0).getCount(), 5, "diamonds in the chest");
    }

    private static void fill(GameTestHelper helper, BlockPos origin, net.minecraft.world.level.block.Block block) {
        for (var x = 0; x < 2; x++) {
            for (var y = 0; y < 2; y++) {
                helper.setBlock(origin.offset(x, y, 0), block);
            }
        }
    }

    private static void assertFilled(GameTestHelper helper, BlockPos origin, net.minecraft.world.level.block.Block block) {
        for (var x = 0; x < 2; x++) {
            for (var y = 0; y < 2; y++) {
                helper.assertBlockPresent(block, origin.offset(x, y, 0));
            }
        }
    }

    private static int execute(GameTestHelper helper, String command) {
        var server = helper.getLevel().getServer();
        try {
            return server.getCommands().getDispatcher().execute(command, server.createCommandSourceStack().withSuppressedOutput());
        } catch (CommandSyntaxException e) {
            throw new GameTestAssertException("/" + command + " failed: " + e.getMessage());
        }
    }
}
