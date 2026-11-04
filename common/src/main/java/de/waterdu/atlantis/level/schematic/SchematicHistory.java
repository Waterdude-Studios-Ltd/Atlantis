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

/**
 * Stores the blocks that a schematic placement replaced, so that the placement can be undone.
 */
public class SchematicHistory {

    private final ServerLevel level;
    private final Deque<PreviousState> history = new ConcurrentLinkedDeque<>();
    private final Set<LevelChunk> chunks = Sets.newConcurrentHashSet();

    /**
     * Creates an empty history.
     *
     * @param level the level of the placement
     */
    protected SchematicHistory(ServerLevel level) {
        this.level = level;
    }

    /**
     * Adds a block as the newest entry and records its chunk.
     *
     * @param state the block state to restore
     * @param blockEntity the block entity to restore, or {@code null} for none
     * @param pos the position of the block
     */
    public void store(BlockState state, BlockEntity blockEntity, BlockPos pos) {
        this.history.offerLast(new PreviousState(state, blockEntity, pos));
        this.chunks.add(this.level.getChunkAt(pos));
    }

    /**
     * Restores the stored blocks in reverse order and empties the history.
     */
    public void restore() {
        while (!this.history.isEmpty()) {
            this.history.pollLast().restore(this.level);
        }
    }

    /**
     * Runs the action for the position of every stored block, from oldest to newest.
     *
     * @param forEach the action, which gets the level and the position
     */
    public void forEachBlockPos(BiConsumer<ServerLevel, BlockPos> forEach) {
        for (var state : this.history) {
            forEach.accept(this.level, state.pos);
        }
    }

    /**
     * Runs the action for every chunk that has a stored block.
     *
     * @param forEach the action, which gets the level and the chunk
     */
    public void forEachChunk(BiConsumer<ServerLevel, LevelChunk> forEach) {
        for (var chunk : this.chunks) {
            forEach.accept(this.level, chunk);
        }
    }

    /**
     * Holds a block that was replaced.
     *
     * @param state the block state before the placement
     * @param blockEntity the block entity before the placement, or {@code null} for none
     * @param pos the position of the block
     */
    protected record PreviousState(BlockState state, BlockEntity blockEntity, BlockPos pos) {

        /**
         * Sets the block back, updated from its neighbors. If the block is set, removes the current block entity and
         * sets the stored one.
         *
         * @param level the level to restore the block in
         */
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
