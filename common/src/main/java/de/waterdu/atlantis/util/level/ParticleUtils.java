package de.waterdu.atlantis.util.level;

import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;

public final class ParticleUtils {

    private ParticleUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

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
