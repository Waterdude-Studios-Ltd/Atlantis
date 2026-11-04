package de.waterdu.atlantis.util.entity;

import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link EntityUtils#getAreaAround}.
 */
@MinecraftBootstrapped
class EntityUtilsTest {

    /**
     * Checks that the area is a cube that extends the radius in each direction from the point.
     */
    @Test
    void areaAroundIsACubeCentredOnThePoint() {
        assertThat(EntityUtils.getAreaAround(1, 2, 3, 2.5)).isEqualTo(new AABB(-1.5, -0.5, 0.5, 3.5, 4.5, 5.5));
    }

    /**
     * Checks that a radius of zero gives an area with no size at the point.
     */
    @Test
    void zeroRadiusIsAPoint() {
        var area = EntityUtils.getAreaAround(1, 2, 3, 0);

        assertThat(area.getSize()).isZero();
        assertThat(area.getCenter().x()).isEqualTo(1);
    }
}
