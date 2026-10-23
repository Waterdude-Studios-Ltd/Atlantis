package de.waterdu.atlantis.util.config;

import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.test.MockServers;
import de.waterdu.atlantis.test.TestModLoader;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class LeaderboardTest {

    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);

    @Test
    void newEntriesAreQueuedUntilRecalculated() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);

        assertThat(leaderboard.update(new Score(A, 5))).isTrue();

        assertThat(leaderboard.queued()).containsEntry(A, 5);
        assertThat(leaderboard.rankings()).isEmpty();
        assertThat(leaderboard.isOnLeaderboard(A)).isTrue();
        assertThat(leaderboard.isOnLeaderboard(B)).isFalse();
        assertThat(leaderboard.positionOf(A)).isZero();
        assertThat(leaderboard.positionOf(B)).isEqualTo(-1);
    }

    @Test
    void recalculateSortsAscending() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);
        leaderboard.update(new Score(A, 30));
        leaderboard.update(new Score(B, 10));
        leaderboard.update(new Score(C, 20));

        leaderboard.recalculate(false).join();

        assertThat(leaderboard.rankingsList()).containsExactly(B, C, A);
        assertThat(leaderboard.queued()).isEmpty();
        assertThat(leaderboard.positionOf(B)).isZero();
        assertThat(leaderboard.positionOf(A)).isEqualTo(2);
    }

    @Test
    void reverseRecalculateSortsDescending() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, true);
        leaderboard.update(new Score(A, 30));
        leaderboard.update(new Score(B, 10));
        leaderboard.update(new Score(C, 20));

        leaderboard.recalculate(false).join();

        assertThat(leaderboard.isReverse()).isTrue();
        assertThat(leaderboard.rankingsList()).containsExactly(A, C, B);
    }

    @Test
    void updatingARankedEntryChangesItInPlace() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);
        leaderboard.update(new Score(A, 1));
        leaderboard.update(new Score(B, 2));
        leaderboard.recalculate(false).join();

        leaderboard.update(new Score(A, 3));

        assertThat(leaderboard.rankings()).containsEntry(A, 3);
        assertThat(leaderboard.queued()).isEmpty();
        assertThat(leaderboard.rankingsList()).containsExactly(A, B);
        leaderboard.recalculate(false).join();
        assertThat(leaderboard.rankingsList()).containsExactly(B, A);
    }

    @Test
    void getterWithArgumentIsCalledWithIt() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "stat", "kills", Integer.class, false);
        leaderboard.update(new Score(A, 7));

        assertThat(leaderboard.queued()).containsEntry(A, "kills".length() + 7);
    }

    @Test
    void unknownGetterIsRejected() {
        assertThatExceptionOfType(NoSuchMethodException.class).isThrownBy(() -> Leaderboard.create("test", Score.class, "missing", Integer.class, false));
    }

    @Test
    void cancelledUpdateIsNotRecorded() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);
        var seen = new ArrayList<Object>();
        Mods.modLoader().event(UEvent.UPDATE_LEADERBOARD).listen(payload -> {
            seen.add(payload.key());
            seen.add(payload.value());
            return InteractionResult.FAIL;
        });

        assertThat(leaderboard.update(new Score(A, 5))).isTrue();

        assertThat(seen).containsExactly(A, 5);
        assertThat(leaderboard.isOnLeaderboard(A)).isFalse();
    }

    @Test
    void cancelledRecalculationLeavesRankingsAlone() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);
        leaderboard.update(new Score(A, 5));
        var posts = new ArrayList<Object>();
        Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_PRE).listen(__ -> InteractionResult.FAIL);
        Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_POST).listen(payload -> {
            posts.add(payload.leaderboard());
            return InteractionResult.PASS;
        });

        leaderboard.recalculate(false).join();

        assertThat(leaderboard.queued()).containsEntry(A, 5);
        assertThat(leaderboard.rankingsList()).isEmpty();
        assertThat(posts).isEmpty();
    }

    @Test
    void recalculationEmitsPostWithTheLeaderboard() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);
        var posts = new ArrayList<Object>();
        Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_POST).listen(payload -> {
            posts.add(payload.leaderboard());
            return InteractionResult.PASS;
        });

        leaderboard.recalculate(false).join();

        assertThat(posts).containsExactly(leaderboard);
    }

    @Test
    void asyncRecalculationCompletes() throws Exception {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);
        leaderboard.update(new Score(A, 2));
        leaderboard.update(new Score(B, 1));

        leaderboard.recalculate(true).get(10, TimeUnit.SECONDS);

        assertThat(leaderboard.rankingsList()).containsExactly(B, A);
    }

    @Test
    void recalculateAllRecalculatesEveryLeaderboardThenEmitsPostAll(TestModLoader loader) throws NoSuchMethodException {
        loader.server(MockServers.executingInline());
        var first = Leaderboard.create("first", Score.class, "points", Integer.class, false);
        var second = Leaderboard.create("second", Score.class, "points", Integer.class, true);
        first.update(new Score(A, 1));
        second.update(new Score(B, 1));
        var postAll = new ArrayList<String>();
        Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_POST_ALL).listen(__ -> {
            postAll.add("post_all");
            return InteractionResult.PASS;
        });

        Leaderboard.recalculateAll(false);

        assertThat(first.rankingsList()).containsExactly(A);
        assertThat(second.rankingsList()).containsExactly(B);
        assertThat(postAll).containsExactly("post_all");
    }

    @Test
    void cancelledRecalculateAllDoesNothing(TestModLoader loader) throws NoSuchMethodException {
        loader.server(MockServers.executingInline());
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);
        leaderboard.update(new Score(A, 1));
        var postAll = new ArrayList<String>();
        Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_PRE_ALL).listen(__ -> InteractionResult.FAIL);
        Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_POST_ALL).listen(__ -> {
            postAll.add("post_all");
            return InteractionResult.PASS;
        });

        Leaderboard.recalculateAll(false);

        assertThat(leaderboard.rankingsList()).isEmpty();
        assertThat(postAll).isEmpty();
    }

    @Test
    void equalityUsesOwnerDataGetterAndValueType() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);

        assertThat(leaderboard).isEqualTo(Leaderboard.create("test", Score.class, "points", Integer.class, true));
        assertThat(leaderboard).hasSameHashCodeAs(Leaderboard.create("test", Score.class, "points", Integer.class, false));
        assertThat(leaderboard).isNotEqualTo(Leaderboard.create("other", Score.class, "points", Integer.class, false));
        assertThat(leaderboard.owner()).isEqualTo("test");
        assertThat(leaderboard.dataClass()).isEqualTo(Score.class);
        assertThat(leaderboard.valueClass()).isEqualTo(Integer.class);
    }

    @Test
    void typeAdapterRoundTripsAndRecalculates() throws NoSuchMethodException {
        var gson = new GsonBuilder().registerTypeHierarchyAdapter(Leaderboard.class, new Leaderboard.TypeAdapter()).create();
        var leaderboard = Leaderboard.create("test", Score.class, "stat", "kills", Integer.class, true);
        leaderboard.update(new Score(A, 1));
        leaderboard.update(new Score(B, 9));
        leaderboard.recalculate(false).join();
        leaderboard.update(new Score(C, 4));

        var json = gson.toJson(leaderboard, Leaderboard.class);
        var copy = (Leaderboard<?, ?>) gson.fromJson(json, Leaderboard.class);

        assertThat(copy).isEqualTo(leaderboard);
        assertThat(copy.isReverse()).isTrue();
        assertThat(copy.rankingsList()).containsExactly(B, C, A);
        assertThat(copy.rankings()).isEqualTo(Map.of(A, 6, B, 14, C, 9));
    }

    public static final class Score implements Data {

        private UUID uuid;
        private final int points;

        public Score(UUID uuid, int points) {
            this.uuid = uuid;
            this.points = points;
        }

        public int points() {
            return this.points;
        }

        public int stat(String name) {
            return name.length() + this.points;
        }

        @Override
        public UUID uuid() {
            return this.uuid;
        }

        @Override
        public void setUUID(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public String identifier() {
            return this.uuid.toString();
        }
    }
}
