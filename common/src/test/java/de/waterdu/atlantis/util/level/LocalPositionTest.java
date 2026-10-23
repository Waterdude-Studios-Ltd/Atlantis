package de.waterdu.atlantis.util.level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
class LocalPositionTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(LocalPosition.class, new LocalPosition.TypeAdapter()).create();

    @Test
    void constructors() {
        assertThat(LocalPosition.ZERO).isEqualTo(new LocalPosition(0, 0, 0));
        assertThat(new LocalPosition(new BlockPos(1, 2, 3))).isEqualTo(new LocalPosition(1, 2, 3));
        var rotated = new LocalPosition(new BlockPos(1, 2, 3), 10, 20);
        assertThat(rotated.xRot()).isEqualTo(10);
        assertThat(rotated.yRot()).isEqualTo(20);
        assertThat(rotated.hasRotation()).isTrue();
        assertThat(new LocalPosition(1, 2, 3).hasRotation()).isFalse();
    }

    @Test
    void settersReturnCopies() {
        var original = new LocalPosition(1, 2, 3, 4, 5);

        assertThat(original.setX(9)).isEqualTo(new LocalPosition(9, 2, 3, 4, 5));
        assertThat(original.setY(9)).isEqualTo(new LocalPosition(1, 9, 3, 4, 5));
        assertThat(original.setZ(9)).isEqualTo(new LocalPosition(1, 2, 9, 4, 5));
        assertThat(original.setXRot(9)).isEqualTo(new LocalPosition(1, 2, 3, 9, 5));
        assertThat(original.setYRot(9)).isEqualTo(new LocalPosition(1, 2, 3, 4, 9));
        assertThat(original).isEqualTo(new LocalPosition(1, 2, 3, 4, 5));
    }

    @Test
    void copiesDoNotShareRotation() {
        var original = new LocalPosition(1, 2, 3, 4, 5);
        var moved = original.add(1, 1, 1);
        var parsed = this.gson.fromJson(this.gson.toJson(moved), LocalPosition.class);

        assertThat(parsed).isEqualTo(new LocalPosition(2, 3, 4, 4, 5));
        assertThat(original.xRot()).isEqualTo(4);
    }

    @Test
    void addMoveAndRotateKeepRotation() {
        var position = new LocalPosition(1, 2, 3, 4, 5);

        assertThat(position.add(1, 1, 1)).isEqualTo(new LocalPosition(2, 3, 4, 4, 5));
        assertThat(position.add(new LocalPosition(1, 0, -1, 90, 90))).isEqualTo(new LocalPosition(2, 2, 2, 4, 5));
        assertThat(position.move(Direction.UP)).isEqualTo(new LocalPosition(1, 3, 3, 4, 5));
        assertThat(position.move(Direction.WEST, 2.5)).isEqualTo(new LocalPosition(-1.5, 2, 3, 4, 5));
        assertThat(position.rotate(1, -1)).isEqualTo(new LocalPosition(1, 2, 3, 5, 4));
    }

    @Test
    void deltas() {
        var position = new LocalPosition(1, 2, 3);

        assertThat(position.getDX(4)).isEqualTo(3);
        assertThat(position.getDY(0)).isEqualTo(-2);
        assertThat(position.getDZ(3)).isZero();
    }

    @Test
    void blockPosContainsThePosition() {
        assertThat(new LocalPosition(1.7, 64.2, 3.9).getBlockPos()).isEqualTo(new BlockPos(1, 64, 3));
        assertThat(new LocalPosition(-0.5, -1.2, -3.9).getBlockPos()).isEqualTo(new BlockPos(-1, -2, -4));
    }

    @RepeatedTest(20)
    void randomizeStaysWithinTheFactor() {
        var randomized = new LocalPosition(10, -20, 30, 1, 2).randomize(0.5);

        assertThat(randomized.x()).isBetween(-5.0, 5.0);
        assertThat(randomized.y()).isBetween(-10.0, 10.0);
        assertThat(randomized.z()).isBetween(-15.0, 15.0);
        assertThat(randomized.xRot()).isEqualTo(1);
        assertThat(randomized.yRot()).isEqualTo(2);
    }

    @Test
    void matchesVectorsAndArrays() {
        var position = new LocalPosition(1, 2, 3, 45, 45);

        assertThat(position.matches(new Vec3(1, 2, 3))).isTrue();
        assertThat(position.matches(new Vector3i(1, 2, 3))).isTrue();
        assertThat(position.matches(new Vector3f(1, 2, 3))).isTrue();
        assertThat(position.matches(new Vector3d(1, 2, 3))).isTrue();
        assertThat(position.matches(new int[]{1, 2, 3})).isTrue();
        assertThat(position.matches(new double[]{1, 2, 3})).isTrue();
        assertThat(position.matches(new long[]{1, 2, 3})).isTrue();
        assertThat(position.matches(new int[]{1, 2})).isFalse();
        assertThat(position.matches(new Vec3(1, 2, 4))).isFalse();
        assertThat(position.matches("1 2 3")).isFalse();
    }

    @Test
    void equalityIncludesRotation() {
        assertThat(new LocalPosition(1, 2, 3, 4, 5))
                .isEqualTo(new LocalPosition(1, 2, 3, 4, 5))
                .hasSameHashCodeAs(new LocalPosition(1, 2, 3, 4, 5))
                .isNotEqualTo(new LocalPosition(1, 2, 3, 4, 6))
                .isNotEqualTo(new Vec3(1, 2, 3));
        assertThat(new LocalPosition(1, 2, 3, 4, 5).toString()).isEqualTo("(1.0, 2.0, 3.0, [4.0, 5.0])");
    }

    @Test
    void typeAdapterOmitsZeroRotation() {
        assertThat(this.gson.toJson(new LocalPosition(1, 2, 3))).isEqualTo("{\"x\":1.0,\"y\":2.0,\"z\":3.0}");
        assertThat(this.gson.toJson(new LocalPosition(1, 2, 3, 4, 5))).isEqualTo("{\"x\":1.0,\"y\":2.0,\"z\":3.0,\"xRot\":4.0,\"yRot\":5.0}");
        assertThat(this.gson.fromJson("{\"x\":1,\"y\":2,\"z\":3}", LocalPosition.class)).isEqualTo(new LocalPosition(1, 2, 3));
        assertThat(this.gson.fromJson("{\"x\":1,\"y\":2,\"z\":3,\"yRot\":5}", LocalPosition.class)).isEqualTo(new LocalPosition(1, 2, 3, 0, 5));
    }
}
