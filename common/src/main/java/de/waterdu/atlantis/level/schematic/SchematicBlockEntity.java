package de.waterdu.atlantis.level.schematic;

import de.waterdu.atlantis.AtlantisLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class SchematicBlockEntity {

    private static final String ID_TAG = "Id";
    private static final String POSITION_TAG = "Pos";

    private final CompoundTag tag;
    private final BlockEntityType<?> type;
    private final BlockPos position;

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

    @Nullable
    protected static SchematicBlockEntity fromTag(String schematic, CompoundTag tag) {
        var blockEntity = new SchematicBlockEntity(schematic, tag);
        if (blockEntity.type == null) {
            return null;
        }
        return blockEntity;
    }

    public BlockPos position() {
        return this.position;
    }

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
