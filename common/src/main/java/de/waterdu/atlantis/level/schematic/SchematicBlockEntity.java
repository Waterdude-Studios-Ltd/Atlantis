package de.waterdu.atlantis.level.schematic;

import de.waterdu.atlantis.AtlantisLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * Holds the data of one block entity in a {@link Schematic}.
 */
public class SchematicBlockEntity {

    private static final String ID_TAG = "Id";
    private static final String POSITION_TAG = "Pos";

    private final CompoundTag tag;
    private final BlockEntityType<?> type;
    private final BlockPos position;

    /**
     * Reads the type and the position from the tag. If they are not valid, logs a warning and leaves them
     * {@code null}.
     *
     * @param schematic the id of the schematic, used in the warning
     * @param tag the block entity tag from the schematic
     */
    private SchematicBlockEntity(String schematic, CompoundTag tag) throws IllegalArgumentException {
        CompoundTag tempTag = null;
        BlockEntityType<?> tempType = null;
        BlockPos tempPosition = null;
        var id = tag.getString(ID_TAG);
        try {
            tempTag = tag;
            tempType = Registries.BLOCK_ENTITY_TYPE.registryFromRegistryKey().get(ResourceLocation.parse(id));
            var posArray = tag.getIntArray(POSITION_TAG);
            if (posArray.length == 3) {
                tempPosition = new BlockPos(posArray[0], posArray[1], posArray[2]);
            } else {
                throw new IllegalArgumentException("Invalid position for BlockEntity: " + tag);
            }
        } catch (Exception e) {
            AtlantisLogger.warn("Invalid block entity in schematic " + schematic + ": " + id);
        }
        this.tag = tempTag;
        this.type = tempType;
        this.position = tempPosition;
    }

    /**
     * Creates a block entity from a tag of the schematic.
     *
     * @param schematic the id of the schematic, used in warnings
     * @param tag the block entity tag from the schematic
     * @return the block entity, or {@code null} if its type is not valid
     */
    @Nullable
    protected static SchematicBlockEntity fromTag(String schematic, CompoundTag tag) {
        var blockEntity = new SchematicBlockEntity(schematic, tag);
        if (blockEntity.type == null) {
            return null;
        }
        return blockEntity;
    }

    /**
     * @return the position relative to the schematic origin, or {@code null} if the tag has no valid position
     */
    public BlockPos position() {
        return this.position;
    }

    /**
     * Loads the tag into the block entity at the position, with the coordinates changed to the position. Creates the
     * block entity first if there is none.
     *
     * @param level the level of the block entity
     * @param pos the position of the block entity in the level
     * @param state the block state at the position
     */
    public void place(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.type == null) {
            return;
        }

        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            blockEntity = this.type.create(pos, state);
            if (blockEntity != null) {
                level.setBlockEntity(blockEntity);
            }
        }
        if (blockEntity != null) {
            var tagCopy = this.tag.copy();
            tagCopy.putInt("x", pos.getX());
            tagCopy.putInt("y", pos.getY());
            tagCopy.putInt("z", pos.getZ());
            blockEntity.loadWithComponents(tagCopy, level.registryAccess());
        }
    }
}
