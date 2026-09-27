package de.waterdu.atlantis.util.entity;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.NumberUtils;
import de.waterdu.atlantis.util.java.RandomUtils;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.level.LocalPosition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class EntityUtils {

    private EntityUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void addEffect(LivingEntity entity, Holder<MobEffect> potion, int duration, int amplifier) {
        EntityUtils.addEffect(entity, potion, duration, amplifier, true, false);
    }

    public static void addEffect(LivingEntity entity, Holder<MobEffect> potion, int duration, int amplifier, boolean ambient, boolean particles) {
        Concurrency.sync(() -> entity.addEffect(new MobEffectInstance(potion, duration, amplifier, ambient, particles)));
    }

    public static void playSound(Player entity, SoundEvent sound) {
        EntityUtils.playSound(entity, sound, SoundSource.MASTER);
    }

    public static void playSound(Player entity, SoundEvent sound, float volume, float pitch) {
        EntityUtils.playSound(entity, sound, SoundSource.MASTER, volume, pitch);
    }

    public static void playSound(Player entity, SoundEvent sound, SoundSource source) {
        EntityUtils.playSound(entity, sound, source, 1F, 1F);
    }

    public static void playSound(Player entity, SoundEvent sound, SoundSource source, float volume, float pitch) {
        entity.playNotifySound(sound, source, volume, pitch);
    }

    public static AABB getAreaAround(Entity entity, double radius) {
        return EntityUtils.getAreaAround(entity.getX(), entity.getY(), entity.getZ(), radius);
    }

    public static AABB getAreaAround(double x, double y, double z, double radius) {
        return new AABB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );
    }

    public static boolean moveToSafeLocation(Entity entity) {
        var level = entity.level();
        var d0 = -1.0D;
        var j = Mth.floor(entity.getX());
        var k = Mth.floor(entity.getY());
        var l = Mth.floor(entity.getZ());
        var i1 = j;
        var j1 = k;
        var k1 = l;
        var i2 = RandomUtils.getRandom().nextInt(4);
        var blockpos$mutableblockpos = new BlockPos.MutableBlockPos(0,0,0);

        for (var j2 = j - 16; j2 <= j + 16; ++j2) {
            var d1 = (double)j2 + 0.5D - entity.getX();

            for (var l2 = l - 16; l2 <= l + 16; ++l2) {
                var d2 = (double)l2 + 0.5D - entity.getZ();
                label293:

                for (var j3 = level.getHeight() - 1; j3 >= 0; --j3) {
                    if (level.getBlockState(blockpos$mutableblockpos.set(j2, j3, l2)).isAir()) {
                        while (j3 > 0 && level.getBlockState(blockpos$mutableblockpos.set(j2, j3 - 1, l2)).isAir()) {
                            --j3;
                        }

                        for (var k3 = i2; k3 < i2 + 4; ++k3) {
                            var l3 = k3 % 2;
                            var i4 = 1 - l3;

                            if (k3 % 4 >= 2) {
                                l3 = -l3;
                                i4 = -i4;
                            }

                            for (var j4 = 0; j4 < 3; ++j4) {
                                for (var k4 = 0; k4 < 4; ++k4) {
                                    for (var l4 = -1; l4 < 4; ++l4) {
                                        var i5 = j2 + (k4 - 1) * l3 + j4 * i4;
                                        var j5 = j3 + l4;
                                        var k5 = l2 + (k4 - 1) * i4 - j4 * l3;
                                        blockpos$mutableblockpos.set(i5, j5, k5);

                                        if (l4 < 0 && !level.getBlockState(blockpos$mutableblockpos).isSolid() || l4 >= 0 && !level.getBlockState(blockpos$mutableblockpos).isAir()) {
                                            continue label293;
                                        }
                                    }
                                }
                            }

                            var d5 = (double) j3 + 0.5D - entity.getY();
                            var d7 = d1 * d1 + d5 * d5 + d2 * d2;

                            if (d0 < 0.0D || d7 < d0) {
                                d0 = d7;
                                i1 = j2;
                                j1 = j3;
                                k1 = l2;
                            }
                        }
                    }
                }
            }
        }

        if (d0 < 0.0D) {
            for (var l5 = j - 16; l5 <= j + 16; ++l5) {
                var d3 = (double) l5 + 0.5D - entity.getX();

                for (var j6 = l - 16; j6 <= l + 16; ++j6) {
                    var d4 = (double) j6 + 0.5D - entity.getZ();
                    label231:

                    for (var i7 = level.getHeight() - 1; i7 >= 0; --i7) {
                        if (level.getBlockState(blockpos$mutableblockpos.set(l5, i7, j6)).isAir()) {
                            while (i7 > 0 && level.getBlockState(blockpos$mutableblockpos.set(l5, i7 - 1, j6)).isAir()) {
                                --i7;
                            }

                            for (var k7 = i2; k7 < i2 + 2; ++k7) {
                                var j8 = k7 % 2;
                                var j9 = 1 - j8;

                                for (var j10 = 0; j10 < 4; ++j10) {
                                    for (var j11 = -1; j11 < 4; ++j11) {
                                        var j12 = l5 + (j10 - 1) * j8;
                                        var i13 = i7 + j11;
                                        var j13 = j6 + (j10 - 1) * j9;
                                        blockpos$mutableblockpos.set(j12, i13, j13);

                                        if (j11 < 0 && !level.getBlockState(blockpos$mutableblockpos).isSolid() || j11 >= 0 && !level.getBlockState(blockpos$mutableblockpos).isAir()) {
                                            continue label231;
                                        }
                                    }
                                }

                                var d6 = (double) i7 + 0.5D - entity.getY();
                                var d8 = d3 * d3 + d6 * d6 + d4 * d4;

                                if (d0 < 0.0D || d8 < d0) {
                                    d0 = d8;
                                    i1 = l5;
                                    j1 = i7;
                                    k1 = j6;
                                }
                            }
                        }
                    }
                }
            }
        }

        var i6 = i1; // X
        var k2 = j1 + 2; // Y
        var k6 = k1; // Z

        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.teleport(i6, k2, k6, entity.getYRot(), entity.getXRot());
        } else {
            entity.absMoveTo(i6, k2, k6);
        }

        return true;
    }

    public static float lookAt(LocalPosition target, LivingEntity entity) {
        var dx = entity.getX() - target.x();
        var dy = entity.getY() - target.y();
        var dz = entity.getZ() - target.z();

        var invSqrt = 1.0 / Math.sqrt(dx*dx + dy*dy + dz*dz);
        dx *= invSqrt;
        dy *= invSqrt;
        dz *= invSqrt;

        var pitch = Math.asin(dy);
        var yaw = Math.atan2(dz, dx);

        pitch = pitch * 180.0 / Math.PI;
        yaw = yaw * 180.0 / Math.PI;

        yaw += 90f;
        entity.setYRot(NumberUtils.boundFloat((float) yaw));
        entity.setXRot(NumberUtils.boundFloat((float) pitch));

        if (entity instanceof ServerPlayer serverPlayer) {
            var packet = new ClientboundPlayerLookAtPacket(EntityAnchorArgument.Anchor.EYES, entity.getX(), entity.getY(), entity.getZ());
            serverPlayer.connection.send(packet);
        }

        return (float) yaw;
    }

    public static void addVelocity(Entity entity, double x, double y, double z) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(x, y, z));
        entity.hasImpulse = true;
    }

    public static void setVelocity(Entity entity, double x, double y, double z) {
        entity.setDeltaMovement(NumberUtils.boundFloat(x), NumberUtils.boundFloat(y), NumberUtils.boundFloat(z));
        entity.hasImpulse = true;
    }

    public static <E extends Entity> Optional<E> createEntity(EntityType<E> type, LevelPosition position, boolean noop, boolean spawn, Consumer<E> modifier) {
        return position.createEntity(type).map(entity -> {
            if (noop) {
                if (type.equals(EntityType.ARMOR_STAND)) {
                    entity.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, NumberUtils.setBit(entity.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS), 8, true));
                }
                entity.setInvulnerable(true);
                entity.setSilent(true);
                entity.setNoGravity(true);
                entity.setInvisible(true);
                entity.noPhysics = true;
            }
            modifier.accept(entity);
            if (spawn) {
                Concurrency.sync(() -> position.level().addFreshEntity(entity));
            }
            return entity;
        });
    }

    public static void getPlayersOrElse(CommandSourceStack source, String arg, Consumer<List<ServerPlayer>> success, Runnable failure) throws CommandSyntaxException {
        List<ServerPlayer> players;
        if (arg.contains("@")) {
            players = new EntitySelectorParser(new StringReader(arg), true).parse().findPlayers(source);
        } else {
            players = new ArrayList<>();
            ServerPlayer target = PlayerReference.get(arg).entityDirect();
            if (target != null) {
                players.add(target);
            }
        }
        if (!players.isEmpty()) {
            success.accept(players);
        } else {
            failure.run();
        }
    }
}
