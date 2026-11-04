package de.waterdu.atlantis.gametest.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestConnections;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.util.config.Configurable;
import de.waterdu.atlantis.util.entity.EntityUtils;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.level.LevelUtils;
import de.waterdu.atlantis.util.level.LocalPosition;
import de.waterdu.atlantis.util.particle.Particle;
import de.waterdu.atlantis.util.particle.ParticleUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Stream;

/**
 * LevelPosition, ConfigurableLevel, LevelUtils, particles and EntityUtils against the live server and its registries.
 */
public final class LevelUtilsGameTest {

    private static final String BATCH = "atlantis_level_utils";
    private static final String OVERWORLD = "minecraft:overworld";
    private static final double EPSILON = 1.0E-4;

    /**
     * Prevents instantiation.
     */
    private LevelUtilsGameTest() {}

    /**
     * Checks that {@link LevelUtils} resolves the loaded levels and their IDs, and returns {@code null} for unknown
     * levels.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void levelUtilsResolveTheLiveLevels(GameTestHelper helper) {
        var overworld = helper.getLevel();

        helper.assertTrue(LevelUtils.overworld() == overworld, "overworld()");
        helper.assertTrue(LevelUtils.level(OVERWORLD) == overworld, "level(String)");
        helper.assertTrue(LevelUtils.level(Level.OVERWORLD) == overworld, "level(ResourceKey)");
        helper.assertTrue(LevelUtils.level(Level.NETHER) != null, "the nether is missing");
        helper.assertTrue(LevelUtils.level("minecraft:not_a_level") == null, "an unknown level resolved");
        helper.assertTrue(LevelUtils.levels().contains(overworld), "levels() misses the overworld");
        helper.assertTrue(LevelUtils.levelIds().contains(OVERWORLD), "levelIds() misses the overworld");
        helper.assertTrue(LevelUtils.levelIds().contains("minecraft:the_nether"), "levelIds() misses the nether");
        helper.assertValueEqual(LevelUtils.levelIds().size(), LevelUtils.levels().size(), "levelIds() size");
        helper.assertTrue(LevelUtils.dimension("") == null, "dimension(\"\") is not null");
        helper.succeed();
    }

    /**
     * Checks that every factory of {@link ConfigurableLevel} gives an equal level that resolves to the overworld, and
     * that {@code matches} and {@code contains} compare correctly.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void configurableLevelResolvesAndMatches(GameTestHelper helper) {
        var level = helper.getLevel();
        var fromString = ConfigurableLevel.of(OVERWORLD);
        var variants = List.of(
                ConfigurableLevel.of("minecraft", "overworld"),
                ConfigurableLevel.of(ResourceLocation.withDefaultNamespace("overworld")),
                ConfigurableLevel.of(Level.OVERWORLD),
                ConfigurableLevel.of(level)
        );

        helper.assertTrue(fromString.get() == level, "get() is not the overworld");
        helper.assertTrue(fromString.key().equals(Level.OVERWORLD), "key()");
        for (var variant : variants) {
            helper.assertValueEqual(variant, fromString, "equality of " + variant);
            helper.assertValueEqual(variant.hashCode(), fromString.hashCode(), "hash code of " + variant);
            helper.assertTrue(variant.get() == level, variant + " does not resolve to the overworld");
        }
        helper.assertTrue(fromString.matches(level), "matches(Level)");
        helper.assertTrue(fromString.matches(Level.OVERWORLD), "matches(ResourceKey)");
        helper.assertTrue(fromString.matches(Level.OVERWORLD.location()), "matches(ResourceLocation)");
        helper.assertTrue(fromString.matches(OVERWORLD), "matches(String)");
        helper.assertTrue(fromString.matches(ConfigurableLevel.of(level)), "matches(ConfigurableLevel)");
        helper.assertFalse(fromString.matches(Level.NETHER), "matches the nether");
        helper.assertFalse(fromString.matches(42), "matches an unrelated object");
        helper.assertFalse(fromString.equals(ConfigurableLevel.of(Level.NETHER)), "equals the nether");

        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 2));
        helper.assertTrue(fromString.contains(pig), "contains() misses an entity in the level");
        helper.assertFalse(ConfigurableLevel.of(Level.NETHER).contains(pig), "the nether contains an overworld entity");
        helper.assertTrue(ConfigurableLevel.of("minecraft:not_a_level").get() == null, "an unknown level resolved");
        helper.succeed();
    }

    /**
     * Checks that a {@link LevelPosition} resolves its level, block position, coordinates and rotation.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void levelPositionResolvesLevelAndBlock(GameTestHelper helper) {
        var target = helper.absoluteVec(new Vec3(2.5, 2.25, 3.75));
        var position = LevelPosition.of(OVERWORLD, target.x, target.y, target.z, 15F, 90F);

        helper.assertTrue(position.level() == helper.getLevel(), "level() is not the overworld");
        helper.assertTrue(position.levelKey().equals(Level.OVERWORLD), "levelKey()");
        helper.assertValueEqual(position.position().getBlockPos(), helper.absolutePos(new BlockPos(2, 2, 3)), "block position");
        helper.assertValueEqual(position.x(), target.x, "x");
        helper.assertValueEqual(position.y(), target.y, "y");
        helper.assertValueEqual(position.z(), target.z, "z");
        helper.assertValueEqual(position.xRot(), 15F, "xRot");
        helper.assertValueEqual(position.yRot(), 90F, "yRot");
        helper.assertBlockPresent(Blocks.AIR, helper.relativePos(position.position().getBlockPos()));
        helper.succeed();
    }

    /**
     * Checks that a {@link LevelPosition} made from an entity has the level, position and rotation of the entity.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void levelPositionOfAnEntityCapturesItsPlace(GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 3));
        pig.setYRot(45F);
        pig.setXRot(-10F);

        var position = LevelPosition.of(pig);

        helper.assertTrue(position.level() == helper.getLevel(), "level()");
        helper.assertValueEqual(position.position(), new LocalPosition(pig), "position()");
        helper.assertValueEqual(position.yRot(), 45F, "yRot");
        helper.assertValueEqual(position.xRot(), -10F, "xRot");
        helper.assertValueEqual(position.position().getBlockPos(), pig.blockPosition(), "block position");
        helper.succeed();
    }

    /**
     * Checks that level positions with the same level and position are equal and have the same hash code, and that
     * positions in different levels are not equal.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void levelPositionsWithTheSameLevelAndPositionAreEqual(GameTestHelper helper) {
        var target = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        var first = LevelPosition.of(OVERWORLD, target.x, target.y, target.z, 5F, 10F);
        var second = LevelPosition.of(ConfigurableLevel.of(helper.getLevel()), new LocalPosition(target.x, target.y, target.z, 5F, 10F));
        var elsewhere = LevelPosition.of("minecraft:the_nether", target.x, target.y, target.z, 5F, 10F);

        helper.assertValueEqual(first, second, "level positions");
        helper.assertValueEqual(first.hashCode(), second.hashCode(), "hash codes");
        helper.assertFalse(first.equals(elsewhere), "positions in different levels are equal");
        helper.succeed();
    }

    /**
     * Checks that {@link LevelPosition#teleport} moves an entity to the position and rotation in the same level.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void levelPositionTeleportsAnEntity(GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        var target = helper.absoluteVec(new Vec3(3.5, 2, 3.5));

        LevelPosition.of(OVERWORLD, target.x, target.y, target.z, 20F, 90F).teleport(pig);

        assertAt(helper, pig, target, "pig");
        helper.assertValueEqual(pig.getYRot(), 90F, "pig yRot");
        helper.assertValueEqual(pig.getXRot(), 20F, "pig xRot");
        helper.assertTrue(pig.level() == helper.getLevel(), "pig changed level");
        helper.succeed();
    }

    /**
     * Checks that {@link LevelPosition#teleport} moves a player that is given by its player reference.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void levelPositionTeleportsAPlayerByReference(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var target = helper.absoluteVec(new Vec3(1.5, 2, 3.5));

        LevelPosition.of(OVERWORLD, target.x, target.y, target.z, 0F, -90F).teleport(player.reference());

        assertAt(helper, player, target, "player");
        helper.assertValueEqual(player.getYRot(), -90F, "player yRot");
        helper.assertTrue(player.level() == helper.getLevel(), "player changed level");
        helper.succeed();
    }

    /**
     * Checks that {@link LevelPosition#createEntity} creates an entity at the position without adding it to the
     * level.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void levelPositionCreatesAnUnspawnedEntity(GameTestHelper helper) {
        var target = helper.absoluteVec(new Vec3(2.5, 2, 2.5));

        var pig = LevelPosition.of(OVERWORLD, target.x, target.y, target.z, 0F, 180F).createEntity(EntityType.PIG).orElse(null);

        helper.assertTrue(pig != null, "no entity was created");
        helper.assertTrue(pig.level() == helper.getLevel(), "entity is in the wrong level");
        assertAt(helper, pig, target, "entity");
        helper.assertValueEqual(pig.getYRot(), 180F, "entity yRot");
        helper.assertTrue(helper.getLevel().getEntity(pig.getUUID()) == null, "entity was added to the level");
        helper.succeed();
    }

    /**
     * Checks that {@link LevelPosition#sound} sends a sound packet with the volume and pitch to a nearby player.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void levelPositionSoundReachesNearbyPlayers(GameTestHelper helper) {
        var player = playerInStructure(helper);
        var target = helper.absoluteVec(new Vec3(2.5, 2.5, 2.5));

        LevelPosition.of(OVERWORLD, target.x, target.y, target.z).sound(SoundEvents.ANVIL_LAND, 0.5F, 1.5F);

        var packet = sounds(player, SoundEvents.ANVIL_LAND)
                .filter(sound -> Math.abs(sound.getX() - target.x) <= 0.5 && Math.abs(sound.getZ() - target.z) <= 0.5)
                .findFirst()
                .orElse(null);
        helper.assertTrue(packet != null, "no sound packet at the position");
        helper.assertValueEqual(packet.getSource(), SoundSource.MASTER, "sound source");
        helper.assertValueEqual(packet.getVolume(), 0.5F, "volume");
        helper.assertValueEqual(packet.getPitch(), 1.5F, "pitch");
        helper.succeed();
    }

    /**
     * Checks that {@link Particle} resolves simple and parameterized particles, caches its data, and gives
     * {@code null} data for unknown particles.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void particleResolvesAgainstTheServerRegistries(GameTestHelper helper) {
        var flame = Particle.of("minecraft:flame");
        var dust = Particle.of("minecraft:dust{color:[1.0,0.0,0.0],scale:2.0}");

        helper.assertTrue(flame.data() != null, "flame did not resolve");
        helper.assertTrue(flame.type() == ParticleTypes.FLAME, "flame type");
        helper.assertTrue(flame.data() == flame.data(), "data() is not cached");
        helper.assertTrue(dust.type() == ParticleTypes.DUST, "dust type");
        helper.assertTrue(dust.data() instanceof DustParticleOptions, "dust options");
        var options = (DustParticleOptions) dust.data();
        helper.assertValueEqual(options.getColor().x(), 1F, "dust red");
        helper.assertValueEqual(options.getColor().y(), 0F, "dust green");
        helper.assertValueEqual(options.getScale(), 2F, "dust scale");
        helper.assertTrue(Particle.of("minecraft:not_a_particle").data() == null, "an unknown particle resolved");
        helper.succeed();
    }

    /**
     * Checks that {@link ParticleUtils#spawnParticle} sends one packet with a count of one for each particle.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void spawnParticleSendsOnePacketPerParticle(GameTestHelper helper) {
        var player = playerInStructure(helper);
        var at = helper.absoluteVec(new Vec3(1.5, 3, 1.5));
        var position = new LocalPosition(at.x, at.y, at.z);

        ParticleUtils.spawnParticle(helper.getLevel(), Particle.of("minecraft:flame").data(), 3, position, LocalPosition.ZERO, 0.25);

        var packets = particlesAt(player, at);
        helper.assertValueEqual(packets.size(), 3, "particle packets");
        for (var packet : packets) {
            helper.assertTrue(packet.getParticle().getType() == ParticleTypes.FLAME, "particle type");
            helper.assertValueEqual(packet.getCount(), 1, "count per packet");
            helper.assertValueEqual(packet.getMaxSpeed(), 0.25F, "speed");
        }
        helper.succeed();
    }

    /**
     * Checks that a negative particle count sends that many packets with a count of zero, and with the motion as the
     * offset.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void negativeParticleCountSendsTheMotionDirectly(GameTestHelper helper) {
        var player = playerInStructure(helper);
        var at = helper.absoluteVec(new Vec3(3.5, 3, 3.5));
        var position = new LocalPosition(at.x, at.y, at.z);
        var motion = new LocalPosition(0.25, 0.5, 0.75);

        ParticleUtils.spawnParticle(helper.getLevel(), ParticleTypes.FLAME, -2,
                position, LocalPosition.ZERO, motion, LocalPosition.ZERO, 1.0);

        var packets = particlesAt(player, at);
        helper.assertValueEqual(packets.size(), 2, "particle packets");
        for (var packet : packets) {
            helper.assertValueEqual(packet.getCount(), 0, "count per packet");
            helper.assertValueEqual(packet.getXDist(), 0.25F, "x motion");
            helper.assertValueEqual(packet.getYDist(), 0.5F, "y motion");
            helper.assertValueEqual(packet.getZDist(), 0.75F, "z motion");
        }
        helper.succeed();
    }

    /**
     * Checks that {@link EntityUtils#addEffect} applies an ambient effect without particles by default, and applies
     * the explicit flags when they are given.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void addEffectAppliesAnAmbientHiddenEffect(GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        var visiblePig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 2, 3));

        EntityUtils.addEffect(pig, MobEffects.MOVEMENT_SPEED, 200, 1);
        EntityUtils.addEffect(visiblePig, MobEffects.MOVEMENT_SPEED, 200, 2, false, true);

        helper.succeedWhen(() -> {
            var effect = pig.getEffect(MobEffects.MOVEMENT_SPEED);
            helper.assertTrue(effect != null, "effect was not applied");
            helper.assertValueEqual(effect.getAmplifier(), 1, "amplifier");
            helper.assertTrue(effect.isAmbient(), "effect is not ambient");
            helper.assertFalse(effect.isVisible(), "effect shows particles");
            var visible = visiblePig.getEffect(MobEffects.MOVEMENT_SPEED);
            helper.assertTrue(visible != null, "explicit effect was not applied");
            helper.assertValueEqual(visible.getAmplifier(), 2, "explicit amplifier");
            helper.assertFalse(visible.isAmbient(), "explicit effect is ambient");
            helper.assertTrue(visible.isVisible(), "explicit effect hides particles");
        });
    }

    /**
     * Checks that {@link EntityUtils#playSound} sends a sound packet with the source, volume and pitch at the player.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void playSoundIsSentToThePlayer(GameTestHelper helper) {
        var player = playerInStructure(helper);

        EntityUtils.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.75F, 1.25F);

        var packet = sounds(player, SoundEvents.EXPERIENCE_ORB_PICKUP).findFirst().orElse(null);
        helper.assertTrue(packet != null, "no sound packet was sent");
        helper.assertValueEqual(packet.getSource(), SoundSource.PLAYERS, "sound source");
        helper.assertValueEqual(packet.getVolume(), 0.75F, "volume");
        helper.assertValueEqual(packet.getPitch(), 1.25F, "pitch");
        helper.assertTrue(Math.abs(packet.getX() - player.getX()) <= 0.5, "sound is not at the player");
        helper.succeed();
    }

    /**
     * Checks that {@link EntityUtils#lookAt} sets the yaw and pitch of an entity towards the target, and returns the
     * yaw.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void lookAtTurnsAnEntityTowardsTheTarget(GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 2, 1));
        var east = new LocalPosition(pig.getX() + 2, pig.getY(), pig.getZ());
        var southAndUp = new LocalPosition(pig.getX(), pig.getY() + 1, pig.getZ() + 1);

        var eastYaw = EntityUtils.lookAt(east, pig);

        helper.assertTrue(close(Mth.wrapDegrees(pig.getYRot()), -90F), "yaw towards +x is " + pig.getYRot());
        helper.assertTrue(close(pig.getXRot(), 0F), "pitch towards a level target is " + pig.getXRot());
        helper.assertTrue(close(Mth.wrapDegrees(eastYaw), -90F), "returned yaw is " + eastYaw);

        EntityUtils.lookAt(southAndUp, pig);

        helper.assertTrue(close(Mth.wrapDegrees(pig.getYRot()), 0F), "yaw towards +z is " + pig.getYRot());
        helper.assertTrue(close(pig.getXRot(), -45F), "pitch towards a raised target is " + pig.getXRot());
        helper.succeed();
    }

    /**
     * Checks that {@link EntityUtils#lookAt} turns a player on the server and sends a look-at packet for the target.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void lookAtTellsThePlayerToLookAtTheTarget(GameTestHelper helper) {
        var player = playerInStructure(helper);
        var target = new LocalPosition(player.getX() + 2, player.getY(), player.getZ());

        EntityUtils.lookAt(target, player);

        helper.assertTrue(close(Mth.wrapDegrees(player.getYRot()), -90F), "server-side yaw is " + player.getYRot());
        var packet = GameTestConnections.sent(player).stream()
                .filter(ClientboundPlayerLookAtPacket.class::isInstance)
                .map(ClientboundPlayerLookAtPacket.class::cast)
                .findFirst()
                .orElse(null);
        helper.assertTrue(packet != null, "no look-at packet was sent");
        var lookingAt = packet.getPosition(helper.getLevel());
        helper.assertTrue(lookingAt != null && lookingAt.distanceTo(target) < EPSILON, "the client is told to look at " + lookingAt + " instead of " + target);
        helper.succeed();
    }

    /**
     * Checks that {@link EntityUtils#createEntity} makes a no-op entity invulnerable, silent, invisible and without
     * gravity or physics, applies the modifier and spawns it.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void createEntityConfiguresAndSpawnsANoopEntity(GameTestHelper helper) {
        var target = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        var position = LevelPosition.of(OVERWORLD, target.x, target.y, target.z);

        var stand = EntityUtils.createEntity(EntityType.ARMOR_STAND, position, true, true, entity -> entity.setCustomName(Component.literal("noop"))).orElse(null);

        helper.assertTrue(stand != null, "no entity was created");
        helper.assertTrue(stand.isInvulnerable(), "not invulnerable");
        helper.assertTrue(stand.isSilent(), "not silent");
        helper.assertTrue(stand.isNoGravity(), "has gravity");
        helper.assertTrue(stand.isInvisible(), "not invisible");
        helper.assertTrue(stand.noPhysics, "has physics");
        helper.assertValueEqual(stand.getCustomName(), Component.literal("noop"), "modifier was not applied");
        assertAt(helper, stand, target, "entity");
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getEntity(stand.getUUID()) == stand, "entity was not spawned"));
    }

    /**
     * Checks that a no-op armor stand from {@link EntityUtils#createEntity} is a marker.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void noopArmorStandIsAMarker(GameTestHelper helper) {
        var target = helper.absoluteVec(new Vec3(1.5, 2, 1.5));

        var stand = EntityUtils.createEntity(EntityType.ARMOR_STAND, LevelPosition.of(OVERWORLD, target.x, target.y, target.z), true, false, entity -> {}).orElseThrow();

        helper.assertTrue(stand.isMarker(), "no-op armour stand is not a marker (flags " + stand.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS) + ")");
        helper.succeed();
    }

    /**
     * Checks that {@link EntityUtils#createEntity} without spawn applies the modifier, keeps a regular entity
     * unchanged and does not add it to the level.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void createEntityWithoutSpawnLeavesTheLevelAlone(GameTestHelper helper) {
        var target = helper.absoluteVec(new Vec3(3.5, 2, 1.5));

        var pig = EntityUtils.createEntity(EntityType.PIG, LevelPosition.of(OVERWORLD, target.x, target.y, target.z), false, false, entity -> entity.setCustomName(Component.literal("pig"))).orElseThrow();

        helper.assertValueEqual(pig.getCustomName(), Component.literal("pig"), "modifier was not applied");
        helper.assertFalse(pig.isInvisible(), "a regular entity is invisible");
        helper.assertFalse(pig.isNoGravity(), "a regular entity has no gravity");
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> helper.assertTrue(helper.getLevel().getEntity(pig.getUUID()) == null, "entity was spawned"))
                .thenSucceed();
    }

    /**
     * Checks that {@link EntityUtils#setVelocity} and {@link EntityUtils#addVelocity} change the delta movement and
     * flag an impulse.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void velocityHelpersSetTheDeltaMovement(GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 2));

        EntityUtils.setVelocity(pig, 0.25, 0.5, -0.75);

        helper.assertValueEqual(pig.getDeltaMovement(), new Vec3(0.25, 0.5, -0.75), "velocity after setVelocity");
        helper.assertTrue(pig.hasImpulse, "setVelocity did not flag an impulse");

        pig.hasImpulse = false;
        EntityUtils.addVelocity(pig, 0.25, 0, 0.25);

        helper.assertValueEqual(pig.getDeltaMovement(), new Vec3(0.5, 0.5, -0.5), "velocity after addVelocity");
        helper.assertTrue(pig.hasImpulse, "addVelocity did not flag an impulse");
        helper.succeed();
    }

    /**
     * Checks that {@link EntityUtils#getAreaAround} gives a box of the correct size with its center on the entity.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void areaAroundAnEntityIsCentredOnIt(GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 2));

        var area = EntityUtils.getAreaAround(pig, 1.5);

        helper.assertValueEqual(area.getCenter(), pig.position(), "centre");
        helper.assertValueEqual(area.getXsize(), 3.0, "x size");
        helper.assertValueEqual(area.getYsize(), 3.0, "y size");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(Pig.class, area).contains(pig), "area does not contain the entity");
        helper.succeed();
    }

    /**
     * Checks that {@link EntityUtils#moveToSafeLocation} moves an entity in the floor to air above a solid block.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void moveToSafeLocationPutsAnEntityOnTheFloor(GameTestHelper helper) {
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 2));
        var floorTop = helper.absolutePos(new BlockPos(2, 2, 2)).getY();

        var moved = EntityUtils.moveToSafeLocation(pig);

        helper.assertTrue(moved, "moveToSafeLocation returned false");
        var feet = pig.blockPosition();
        helper.assertValueEqual(feet.getY(), floorTop, "feet height (entity at " + pig.position() + ")");
        helper.assertTrue(helper.getLevel().getBlockState(feet).isAir(), "feet are not in air");
        helper.assertTrue(helper.getLevel().getBlockState(feet.below()).isSolid(), "nothing solid below the feet");
        helper.succeed();
    }

    /**
     * Checks that the default {@link Configurable} contexts deserialize and serialize potions, particle types,
     * particles, effects, sounds and biomes.
     *
     * @param helper the GameTest helper
     */
    @AtlantisGameTest(batch = BATCH)
    public static void defaultConfigurableContextsRoundTripRegistryValues(GameTestHelper helper) {
        var builder = new GsonBuilder();
        Configurable.registerDefaultContexts(builder);
        var gson = builder.create();

        helper.assertTrue(deserialize(gson, "\"minecraft:swiftness\"", Potion.class) == Potions.SWIFTNESS.value(), "Potion");
        helper.assertTrue(deserialize(gson, "\"minecraft:flame\"", ParticleType.class) == ParticleTypes.FLAME, "ParticleType");

        var particle = deserialize(gson, "{\"type\":\"minecraft:flame\",\"parameters\":{\"speed\":0.5}}", Particle.class);
        helper.assertValueEqual(particle.particle(), "minecraft:flame", "Particle id");
        helper.assertValueEqual(particle.parameter(Particle.SPEED), 0.5, "Particle speed");
        helper.assertTrue(particle.type() == ParticleTypes.FLAME, "Particle type");
        helper.assertTrue(deserialize(gson, "\"minecraft:flame\"", Particle.class).type() == ParticleTypes.FLAME, "Particle from a string");

        helper.assertValueEqual(serialize(gson, Potions.SWIFTNESS.value(), Potion.class), "minecraft:swiftness", "Potion round trip");
        helper.assertValueEqual(serialize(gson, ParticleTypes.FLAME, ParticleType.class), "minecraft:flame", "ParticleType round trip");
        helper.assertValueEqual(serialize(gson, MobEffects.MOVEMENT_SPEED.value(), MobEffect.class), "minecraft:speed", "MobEffect round trip");
        helper.assertValueEqual(serialize(gson, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundEvent.class), "minecraft:entity.experience_orb.pickup", "SoundEvent round trip");
        var plains = deserialize(gson, "\"minecraft:plains\"", Biome.class);
        helper.assertValueEqual(serialize(gson, plains, Biome.class), "minecraft:plains", "Biome round trip");
        helper.assertValueEqual(serialize(gson, Particle.of("minecraft:flame"), Particle.class), "minecraft:flame", "Particle without parameters");
        var withParameters = JsonParser.parseString(gson.toJson(Configurable.of(Particle.of("minecraft:flame").withParameter(Particle.SPEED, 2)), configurableOf(Particle.class))).getAsJsonObject();
        helper.assertValueEqual(withParameters.get("particle").getAsString(), "minecraft:flame", "Particle with parameters id");
        helper.assertValueEqual(withParameters.getAsJsonObject("parameters").get(Particle.SPEED).getAsDouble(), 2.0, "Particle with parameters speed");
        helper.succeed();
    }

    /**
     * Creates a mock player in the center of the test structure and drains the packets that were sent to it.
     *
     * @param helper the GameTest helper
     * @return the mock player
     */
    private static ServerPlayer playerInStructure(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var at = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        player.teleportTo(helper.getLevel(), at.x, at.y, at.z, 0F, 0F);
        GameTestConnections.sent(player);
        return player;
    }

    /**
     * Drains the packets sent to the player and keeps the sound packets of the sound.
     *
     * @param player the player to read the packets of
     * @param sound the sound to look for
     * @return the sound packets of the sound
     */
    private static Stream<ClientboundSoundPacket> sounds(ServerPlayer player, SoundEvent sound) {
        return GameTestConnections.sent(player).stream()
                .filter(ClientboundSoundPacket.class::isInstance)
                .map(ClientboundSoundPacket.class::cast)
                .filter(packet -> packet.getSound().value() == sound);
    }

    /**
     * Drains the packets sent to the player and keeps the particle packets at the position.
     *
     * @param player the player to read the packets of
     * @param at the position of the particles
     * @return the particle packets at the position
     */
    private static List<ClientboundLevelParticlesPacket> particlesAt(ServerPlayer player, Vec3 at) {
        return GameTestConnections.sent(player).stream()
                .filter(ClientboundLevelParticlesPacket.class::isInstance)
                .map(ClientboundLevelParticlesPacket.class::cast)
                .filter(packet -> new Vec3(packet.getX(), packet.getY(), packet.getZ()).distanceTo(at) < EPSILON)
                .toList();
    }

    /**
     * Fails the test if the entity is not at the expected position.
     *
     * @param helper the GameTest helper
     * @param entity the entity to check
     * @param expected the expected position
     * @param name the name of the entity in the failure message
     */
    private static void assertAt(GameTestHelper helper, Entity entity, Vec3 expected, String name) {
        helper.assertTrue(entity.position().distanceTo(expected) < EPSILON, name + " is at " + entity.position() + " instead of " + expected);
    }

    /**
     * Checks if two angles differ by less than {@code 0.01}.
     *
     * @param actual the actual angle
     * @param expected the expected angle
     * @return {@code true} if the angles are close
     */
    private static boolean close(float actual, float expected) {
        return Math.abs(actual - expected) < 0.01F;
    }

    /**
     * Gets the generic type of a {@link Configurable} of the class.
     *
     * @param type the class of the value
     * @return the parameterized {@link Configurable} type
     */
    private static Type configurableOf(Class<?> type) {
        return TypeToken.getParameterized(Configurable.class, type).getType();
    }

    /**
     * Deserializes the JSON as a {@link Configurable} of the type and gets its value.
     *
     * @param gson the Gson instance to use
     * @param json the JSON to read
     * @param type the class of the value
     * @param <T> the type of the value
     * @return the value of the deserialized configurable
     */
    private static <T> T deserialize(Gson gson, String json, Class<T> type) {
        Configurable<T> configurable = gson.fromJson(json, configurableOf(type));
        return configurable.get();
    }

    /**
     * Serializes the value as a {@link Configurable} of the type and reads the result as a string.
     *
     * @param gson the Gson instance to use
     * @param value the value to write
     * @param type the class of the value
     * @param <T> the type of the value
     * @return the serialized value as a string
     */
    private static <T> String serialize(Gson gson, T value, Class<T> type) {
        return JsonParser.parseString(gson.toJson(Configurable.of(value), configurableOf(type))).getAsString();
    }
}
