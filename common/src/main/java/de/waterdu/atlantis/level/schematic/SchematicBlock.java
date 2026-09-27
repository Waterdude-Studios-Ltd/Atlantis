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

import javax.annotation.Nullable;
import java.util.HashSet;

public class SchematicBlock {

    private static final HolderLookup<Block> LOOKUP = Registries.BLOCK.registryFromRegistryKey().asLookup();

    private final BlockInput input;

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
