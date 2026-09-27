package de.waterdu.atlantis.level.schematic;

import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;

public class SchematicHistory {

    private final ServerLevel level;
    private final Deque<PreviousState> history = new ConcurrentLinkedDeque<>();
    private final Set<LevelChunk> chunks = Sets.newConcurrentHashSet();

    protected SchematicHistory(ServerLevel level) {
        this.level = level;
    }

    public void store(BlockState state, BlockEntity blockEntity, BlockPos pos) {
        this.history.offerLast(new PreviousState(state, blockEntity, pos));
        this.chunks.add(this.level.getChunkAt(pos));
    }

    public void restore() {
        while (!this.history.isEmpty()) {
            this.history.pollLast().restore(this.level);
        }
    }

    public void forEachBlockPos(BiConsumer<ServerLevel, BlockPos> forEach) {
        for (var state : this.history) {
            forEach.accept(this.level, state.pos);
        }
    }

    public void forEachChunk(BiConsumer<ServerLevel, LevelChunk> forEach) {
        for (var chunk : this.chunks) {
            forEach.accept(this.level, chunk);
        }
    }

    protected record PreviousState(BlockState state, BlockEntity blockEntity, BlockPos pos) {

        public void restore(ServerLevel level) {
            var restoredState = Block.updateFromNeighbourShapes(this.state, level, this.pos);
            if (restoredState.isAir()) {
                restoredState = this.state;
            }
            if (level.setBlock(this.pos, restoredState, 2)) {
                var currentBlockEntity = level.getBlockEntity(this.pos);
                if (currentBlockEntity != null) {
                    level.removeBlockEntity(this.pos);
                }
                if (this.blockEntity != null) {
                    level.setBlockEntity(this.blockEntity);
                }
            }
        }
    }
}
