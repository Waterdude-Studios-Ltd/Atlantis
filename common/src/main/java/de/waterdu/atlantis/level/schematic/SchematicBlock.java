package de.waterdu.atlantis.level.schematic;

import de.waterdu.atlantis.AtlantisLogger;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;
import java.util.HashSet;

/**
 * Holds one block state from the palette of a {@link Schematic}.
 */
public class SchematicBlock {

    private static final HolderLookup<Block> LOOKUP = Registries.BLOCK.registryFromRegistryKey().asLookup();

    private final BlockInput input;

    /**
     * Parses the block state string. If it is not valid, logs a warning and uses air.
     *
     * @param schematic the id of the schematic, used in the warning
     * @param block the block state string from the palette
     */
    protected SchematicBlock(String schematic, String block) {
        BlockInput inputTemp;
        try {
            var result = BlockStateParser.parseForBlock(LOOKUP, block, true);
            if (result.blockState() == null) {
                throw new NullPointerException("Null BlockState in schematic: " + block);
            }
            inputTemp = new BlockInput(result.blockState(), result.properties().keySet(), result. nbt());
        } catch (Exception e) {
            AtlantisLogger.warn("Invalid block in schematic " + schematic + ": " + block);
            inputTemp = new BlockInput(Blocks.AIR.defaultBlockState(), new HashSet<>(), null);
        }
        this.input = inputTemp;
    }

    /**
     * Stores the current block in the history, then places this block, updated from its neighbors. If the block is
     * set, also places the block entity.
     *
     * @param level the level to place the block in
     * @param pos the position of the block
     * @param blockEntity the block entity to place, or {@code null} for none
     * @param ignoreAir does not place the block if it is air. The current block is still stored in the history.
     * @param history the history that stores the replaced block
     */
    public void place(ServerLevel level, BlockPos pos, @Nullable SchematicBlockEntity blockEntity, boolean ignoreAir, SchematicHistory history) {
        history.store(level.getBlockState(pos), level.getBlockEntity(pos), pos);
        BlockState state = Block.updateFromNeighbourShapes(this.input.getState(), level, pos);
        if (state.isAir()) {
            state = this.input.getState();
        }
        if (ignoreAir && state.isAir()) {
            return;
        }
        if (level.setBlock(pos, state, 2) && blockEntity != null) {
            blockEntity.place(level, pos, state);
        }
    }
}
