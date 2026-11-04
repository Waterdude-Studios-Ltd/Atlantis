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

/**
 * Tests {@link Leaderboard} updates, recalculation, events and the type adapter.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class LeaderboardTest {

    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);

    /**
     * Checks that a new entry is queued and not ranked, but still counts as on the leaderboard.
     */
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

    /**
     * Checks that a recalculation empties the queue and sorts the entries from lowest to highest.
     */
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

    /**
     * Checks that a reverse leaderboard sorts the entries from highest to lowest.
     */
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

    /**
     * Checks that an update to a ranked entry changes its value but not its position until the next recalculation.
     */
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

    /**
     * Checks that a getter with an argument is called with the argument given to the leaderboard.
     */
    @Test
    void getterWithArgumentIsCalledWithIt() throws NoSuchMethodException {
        var leaderboard = Leaderboard.create("test", Score.class, "stat", "kills", Integer.class, false);
        leaderboard.update(new Score(A, 7));

        assertThat(leaderboard.queued()).containsEntry(A, "kills".length() + 7);
    }

    /**
     * Checks that a leaderboard with a missing getter throws {@link NoSuchMethodException}.
     */
    @Test
    void unknownGetterIsRejected() {
        assertThatExceptionOfType(NoSuchMethodException.class).isThrownBy(() -> Leaderboard.create("test", Score.class, "missing", Integer.class, false));
    }

    /**
     * Checks that the update event gets the key and the value, and that a cancelled update does not add the entry.
     */
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

    /**
     * Checks that a cancelled pre-recalculation event keeps the queue and the rankings, and fires no post event.
     */
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

    /**
     * Checks that a recalculation fires the post event with the leaderboard.
     */
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

    /**
     * Checks that an async recalculation completes and sorts the entries.
     */
    @Test
    void asyncRecalculationCompletes() throws Exception {
        var leaderboard = Leaderboard.create("test", Score.class, "points", Integer.class, false);
        leaderboard.update(new Score(A, 2));
        leaderboard.update(new Score(B, 1));

        leaderboard.recalculate(true).get(10, TimeUnit.SECONDS);

        assertThat(leaderboard.rankingsList()).containsExactly(B, A);
    }

    /**
     * Checks that {@code recalculateAll} recalculates every leaderboard and then fires the post-all event once.
     *
     * @param loader the test mod loader that gets the inline mock server
     */
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

    /**
     * Checks that a cancelled pre-all event stops every recalculation and the post-all event.
     *
     * @param loader the test mod loader that gets the inline mock server
     */
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

    /**
     * Checks that equality ignores the reverse flag but uses the owner, and that the accessors return the given
     * values.
     */
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

    /**
     * Checks that a leaderboard read back with {@link Leaderboard.TypeAdapter} is equal, keeps the reverse flag and
     * ranks the queued entries too.
     */
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

    /**
     * Holds the points of a player for the leaderboard tests.
     */
    public static final class Score implements Data {

        private UUID uuid;
        private final int points;

        /**
         * Creates a score.
         *
         * @param uuid the UUID of the player
         * @param points the points of the player
         */
        public Score(UUID uuid, int points) {
            this.uuid = uuid;
            this.points = points;
        }

        /**
         * @return the points of the player
         */
        public int points() {
            return this.points;
        }

        /**
         * Gets a stat that adds the length of the name to the points.
         *
         * @param name the name of the stat
         * @return the length of the name plus the points
         */
        public int stat(String name) {
            return name.length() + this.points;
        }

        /**
         * @return the UUID of the player
         */
        @Override
        public UUID uuid() {
            return this.uuid;
        }

        /**
         * Sets the UUID of the player.
         *
         * @param uuid the new UUID
         */
        @Override
        public void setUUID(UUID uuid) {
            this.uuid = uuid;
        }

        /**
         * @return the UUID of the player as a string
         */
        @Override
        public String identifier() {
            return this.uuid.toString();
        }
    }
}
