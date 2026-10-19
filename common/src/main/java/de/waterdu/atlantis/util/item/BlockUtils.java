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

public final class BlockUtils {

    private BlockUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static Optional<Block> getBlock(String block) {
        return BlockUtils.getBlock(block, true);
    }

    public static Optional<Block> getBlock(String block, boolean airIsNull) {
        return BlockUtils.getBlock(ResourceLocation.tryParse(block), airIsNull);
    }

    public static Optional<Block> getBlock(ResourceLocation location) {
        return BlockUtils.getBlock(location, true);
    }

    public static Optional<Block> getBlock(ResourceLocation location, boolean airIsNull) {
        var block = location == null ? null : Registries.BLOCK.registryFromRegistryKey().getOptional(location).orElse(null);
        if (block == null || (airIsNull && block == Blocks.AIR)) {
            return Optional.empty();
        }
        return Optional.of(block);
    }

    public static boolean setBlock(Player player, BlockPos pos, BlockState state, boolean ifAir) {
        return BlockUtils.setBlock(player, pos, state, ifAir, false);
    }

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

    public static boolean canPlaceBlock(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer serverPlayer) {
            return !player.blockActionRestricted(serverPlayer.level(), pos, serverPlayer.gameMode.getGameModeForPlayer());
        }
        return false;
    }

    public static boolean canRemoveBlock(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer serverPlayer) {
            var level = serverPlayer.level();
            var state = level.getBlockState(pos);
            return Mods.modLoader().onBlockBreak(serverPlayer, pos, state);
        }
        return false;
    }

    public static boolean removeBlock(Player player, BlockPos pos) {
        return BlockUtils.removeBlock(player, pos, true);
    }

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

    private static boolean removeBlock0(Level level, Player player, BlockPos pos, boolean canHarvest) {
        BlockState state = level.getBlockState(pos);
        // Mojang 1.21.1: onDestroyedByPlayer is NeoForge-only; use removeBlock + destroy.
        boolean removed = level.removeBlock(pos, false);
        if (removed) {
            state.getBlock().destroy(level, pos, state);
        }
        return removed;
    }

    public static void animateBlockBreak(Level level, BlockPos pos, BlockState state) {
        level.levelEvent(2001, pos, Block.getId(state));
    }
}
