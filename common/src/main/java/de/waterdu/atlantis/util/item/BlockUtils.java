package de.waterdu.atlantis.util.item;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Has utility methods to find, place and break blocks.
 */
public final class BlockUtils {

    private BlockUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Finds the block with the ID in the block registry. Air counts as not found.
     *
     * @param block the ID of the block
     * @return the block, or empty if the ID is not valid, not registered or air
     */
    public static Optional<Block> getBlock(String block) {
        return BlockUtils.getBlock(block, true);
    }

    /**
     * Finds the block with the ID in the block registry.
     *
     * @param block the ID of the block
     * @param airIsNull if {@code true}, air counts as not found
     * @return the block, or empty if the ID is not valid or not registered
     */
    public static Optional<Block> getBlock(String block, boolean airIsNull) {
        return BlockUtils.getBlock(ResourceLocation.tryParse(block), airIsNull);
    }

    /**
     * Finds the block with the location in the block registry. Air counts as not found.
     *
     * @param location the ID of the block
     * @return the block, or empty if the location is null, not registered or air
     */
    public static Optional<Block> getBlock(ResourceLocation location) {
        return BlockUtils.getBlock(location, true);
    }

    /**
     * Finds the block with the location in the block registry.
     *
     * @param location the ID of the block
     * @param airIsNull if {@code true}, air counts as not found
     * @return the block, or empty if the location is null or not registered
     */
    public static Optional<Block> getBlock(ResourceLocation location, boolean airIsNull) {
        var block = location == null ? null : Registries.BLOCK.registryFromRegistryKey().getOptional(location).orElse(null);
        if (block == null || (airIsNull && block == Blocks.AIR)) {
            return Optional.empty();
        }
        return Optional.of(block);
    }

    /**
     * {@link #setBlock(Player, BlockPos, BlockState, boolean, boolean)} with the checks of {@link #canPlaceBlock}.
     *
     * @param player the player who places the block
     * @param pos the position of the block
     * @param state the block state to place
     * @param ifAir places the block only if the position contains air
     * @return {@code true} if the block was placed
     */
    public static boolean setBlock(Player player, BlockPos pos, BlockState state, boolean ifAir) {
        return BlockUtils.setBlock(player, pos, state, ifAir, false);
    }

    /**
     * Places the block as the player. This sends the change to clients but does not update neighbor blocks.
     *
     * @param ifAir places the block only if the position contains air
     * @param ignoreChecks skips {@link #canPlaceBlock}. The loader place event still fires.
     * @return {@code true} if the block was placed. Always {@code false} if the player is not a {@link ServerPlayer}.
     */
    public static boolean setBlock(Player player, BlockPos pos, BlockState state, boolean ifAir, boolean ignoreChecks) {
        if (player instanceof ServerPlayer serverPlayer) {
            var level = serverPlayer.level();
            if (ifAir) {
                if (!level.getBlockState(pos).isAir()) {
                    return false;
                }
            }
            if (ignoreChecks || BlockUtils.canPlaceBlock(player, pos)) {
                if (Mods.modLoader().onBlockPlace(serverPlayer, pos)) {
                    return false;
                }
                level.setBlock(pos, state, 2);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks only the game mode restrictions, such as adventure and spectator mode.
     * It does not check spawn protection or fire an event.
     */
    public static boolean canPlaceBlock(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer serverPlayer) {
            return !player.blockActionRestricted(serverPlayer.level(), pos, serverPlayer.gameMode.getGameModeForPlayer());
        }
        return false;
    }

    /**
     * Fires the loader block break event. It does not remove the block.
     *
     * @return {@code false} if a listener canceled the event or if the player is not a {@link ServerPlayer}
     */
    public static boolean canRemoveBlock(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer serverPlayer) {
            var level = serverPlayer.level();
            var state = level.getBlockState(pos);
            return Mods.modLoader().onBlockBreak(serverPlayer, pos, state);
        }
        return false;
    }

    /**
     * {@link #removeBlock(Player, BlockPos, boolean)} with drops.
     *
     * @param player the player who breaks the block
     * @param pos the position of the block
     * @return {@code false} if the player cannot break the block or is not a {@link ServerPlayer}
     */
    public static boolean removeBlock(Player player, BlockPos pos) {
        return BlockUtils.removeBlock(player, pos, true);
    }

    /**
     * Breaks the block as if the player mined it.
     * This damages the held item, shows the break effect and fires the loader events.
     * It does not fire the event of {@link #canRemoveBlock}.
     *
     * @param attemptDrop drops the items of the block if the player can harvest it. Players in creative mode get no
     *     drops.
     * @return {@code false} if the player cannot break the block or is not a {@link ServerPlayer}
     */
    public static boolean removeBlock(Player player, BlockPos pos, boolean attemptDrop) {
        if (player instanceof ServerPlayer serverPlayer) {
            var level = serverPlayer.level();
            var state = level.getBlockState(pos);
            var block = state.getBlock();
            if (Mods.modLoader().onEntityDestroyBlock(serverPlayer, pos, state)) {
                var blockEntity = level.getBlockEntity(pos);
                if ((block instanceof CommandBlock || block instanceof StructureBlock || block instanceof JigsawBlock) && !player.canUseGameMasterBlocks()) {
                    level.sendBlockUpdated(pos, state, state, 3);
                    return false;
                } else if (player.blockActionRestricted(level, pos, serverPlayer.gameMode.getGameModeForPlayer())) {
                    return false;
                } else {
                    if (player.isCreative()) {
                        if (BlockUtils.removeBlock0(level, player, pos, false)) {
                            Mods.modLoader().afterBlockBreak(serverPlayer, pos, state, blockEntity);
                        }
                        BlockUtils.animateBlockBreak(level, pos, state);
                    } else {
                        ItemStack itemstack = player.getMainHandItem();
                        ItemStack itemstack1 = itemstack.copy();
                        boolean flag1 = player.hasCorrectToolForDrops(state) || Mods.modLoader().canHarvest(serverPlayer, pos, state);
                        itemstack.mineBlock(level, state, pos, player);
                        if (itemstack.isEmpty() && !itemstack1.isEmpty()) {
                            Mods.modLoader().onPlayerDestroyItem(serverPlayer, itemstack1, InteractionHand.MAIN_HAND);
                        }
                        boolean flag = BlockUtils.removeBlock0(level, player, pos, flag1);
                        if (flag) {
                            Mods.modLoader().afterBlockBreak(serverPlayer, pos, state, blockEntity);
                        }
                        BlockUtils.animateBlockBreak(level, pos, state);
                        if (attemptDrop && flag && flag1) {
                            block.playerDestroy(level, player, pos, state, blockEntity, itemstack1);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the block without drops, then calls {@link Block#destroy} if the block was removed.
     *
     * @param level the level of the block
     * @param player the player who breaks the block. It is not used.
     * @param pos the position of the block
     * @param canHarvest if the player can harvest the block. It is not used.
     * @return {@code true} if the block was removed
     */
    private static boolean removeBlock0(Level level, Player player, BlockPos pos, boolean canHarvest) {
        BlockState state = level.getBlockState(pos);
        // Mojang 1.21.1: onDestroyedByPlayer is NeoForge-only; use removeBlock + destroy.
        boolean removed = level.removeBlock(pos, false);
        if (removed) {
            state.getBlock().destroy(level, pos, state);
        }
        return removed;
    }

    /**
     * Shows the break particles and plays the break sound to nearby players. It does not change the block.
     */
    public static void animateBlockBreak(Level level, BlockPos pos, BlockState state) {
        level.levelEvent(2001, pos, Block.getId(state));
    }
}
