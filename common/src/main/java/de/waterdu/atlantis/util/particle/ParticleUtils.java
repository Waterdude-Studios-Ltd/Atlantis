package de.waterdu.atlantis.util.particle;

import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.level.LocalPosition;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;

/**
 * Spawns particles in a level.
 */
public final class ParticleUtils {

    /**
     * Throws an exception, because this class only has static methods.
     */
    private ParticleUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Sends each particle separately to the players near the position, with zero motion.
     *
     * @param level the level to spawn the particles in
     * @param particle the particle to spawn
     * @param count the number of particles. If it is negative, the particles do not move. Otherwise, the client uses
     *     {@code speed} as a random velocity.
     * @param position the position of the particles
     * @param positionRandomization the maximum random offset from the position on each axis
     * @param speed the speed of the particles
     */
    public static void spawnParticle(ServerLevel level, ParticleOptions particle, int count, LocalPosition position,
                                     LocalPosition positionRandomization, double speed) {
        ParticleUtils.spawnParticle(
                level,
                particle,
                count,
                position,
                positionRandomization,
                LocalPosition.ZERO,
                LocalPosition.ZERO,
                speed
        );
    }

    /**
     * Sends each particle separately to the players near the position.
     *
     * @param count the number of particles. If it is negative, the client moves each particle with the velocity
     *     {@code motion} times {@code speed}. Otherwise, the client uses {@code motion} as a random spread around the
     *     position, and {@code speed} as a random velocity.
     * @param positionRandomization the maximum random offset from the position on each axis
     * @param motionRandomization the maximum random offset added to {@code motion} on each axis
     */
    public static void spawnParticle(ServerLevel world, ParticleOptions particle, int count, LocalPosition position,
                                     LocalPosition positionRandomization, LocalPosition motion, LocalPosition motionRandomization,
                                     double speed) {
        var rgb = count < 0;
        if (rgb) {
            count = -count;
        }
        for (var i = 0; i < count; i++) {
            var positionOffset = positionRandomization.randomize();
            var motionOffset = motionRandomization.randomize();
            world.sendParticles(particle,
                    position.x + positionOffset.x,
                    position.y + positionOffset.y,
                    position.z + positionOffset.z,
                    rgb ? 0 : 1,
                    motion.x + motionOffset.x,
                    motion.y + motionOffset.y,
                    motion.z + motionOffset.z,
                    speed
            );
        }
    }
}
