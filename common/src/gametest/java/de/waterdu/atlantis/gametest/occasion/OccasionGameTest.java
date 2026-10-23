package de.waterdu.atlantis.gametest.occasion;

import com.mojang.authlib.GameProfile;
import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.occasion.Moment;
import de.waterdu.atlantis.occasion.Occasion;
import de.waterdu.atlantis.occasion.OccasionManager;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.level.LocalPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Occasions and their moments on a live server. Occasions are ticked by the server and live in a global manager, so
 * each test builds its own.
 */
public final class OccasionGameTest {

    private static final String BATCH = "atlantis_occasions";

    private OccasionGameTest() {}

    @AtlantisGameTest(batch = BATCH)
    public static void buildingStartsTheFirstMomentOnce(GameTestHelper helper) {
        var firstStarts = new AtomicInteger();
        var secondStarts = new AtomicInteger();

        var occasion = Occasion.builder()
                .newMoment().onStart(__ -> firstStarts.incrementAndGet()).build()
                .newMoment().onStart(__ -> secondStarts.incrementAndGet()).build()
                .build();

        helper.assertValueEqual(firstStarts.get(), 1, "starts of the first moment");
        helper.assertValueEqual(secondStarts.get(), 0, "starts of the second moment");
        helper.assertValueEqual(occasion.momentCount(), 2, "moments");
        helper.assertTrue(OccasionManager.getFor(occasion.uuid()).orElse(null) == occasion, "occasion not registered");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void membersAreTrackedByTheManager(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var reference = PlayerReference.get(player);
        var recovery = position(helper, new BlockPos(1, 2, 1));
        var occasion = Occasion.builder().recoveryPosition(recovery).newMoment().build().build();

        helper.assertValueEqual(occasion.addMembers(player), 1, "added members");
        helper.assertValueEqual(occasion.addMembers(player), 0, "added the same member twice");

        helper.assertTrue(occasion.isMember(player), "not a member");
        helper.assertTrue(OccasionManager.getFor(reference).orElse(null) == occasion, "the manager doesn't know the member's occasion");
        helper.assertTrue(OccasionManager.isIn(reference), "isIn");

        helper.assertTrue(occasion.removeMember(player), "removeMember");
        helper.assertFalse(OccasionManager.getFor(reference).isPresent(), "the manager still has the member's occasion");
        helper.assertFalse(OccasionManager.isIn(reference), "isIn after removal");
        assertAt(helper, player, recovery);
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void theServerTicksTheOccasionAndItsMoment(GameTestHelper helper) {
        var occasionTicks = new AtomicInteger();
        var momentTicks = new AtomicInteger();
        var occasion = Occasion.builder()
                .onTick((tick, __) -> occasionTicks.incrementAndGet())
                .newMoment().duration(5).onTick((tick, __) -> momentTicks.incrementAndGet()).build()
                .build();
        var moment = occasion.currentMoment();

        helper.assertFalse(moment.exceededDuration(), "exceeded its duration before ticking");
        helper.succeedWhen(() -> {
            helper.assertTrue(moment.exceededDuration(), "moment did not exceed its duration of 5 ticks");
            helper.assertTrue(occasion.currentTick() >= 5, "occasion ticked " + occasion.currentTick() + " times");
            helper.assertValueEqual(occasionTicks.get(), (int) occasion.currentTick(), "occasion onTick calls");
            helper.assertValueEqual(momentTicks.get(), (int) moment.currentTick(), "moment onTick calls");
        });
    }

    @AtlantisGameTest(batch = BATCH)
    public static void incrementingPastTheLastMomentCompletesTheOccasion(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var ends = new AtomicInteger();
        var completions = new AtomicInteger();
        var occasion = Occasion.builder()
                .recoveryPosition(position(helper, new BlockPos(1, 2, 1)))
                .onComplete(__ -> completions.incrementAndGet())
                .newMoment().onEnd(__ -> ends.incrementAndGet()).build()
                .build();
        occasion.addMembers(player);

        helper.assertTrue(occasion.incrementMoment(), "incrementMoment did not complete the occasion");

        helper.assertTrue(occasion.completed(), "not completed");
        helper.assertValueEqual(ends.get(), 1, "onEnd calls");
        helper.assertValueEqual(completions.get(), 1, "onComplete calls");
        helper.assertValueEqual(occasion.memberCount(), 0, "members after completion");
        helper.assertFalse(OccasionManager.isIn(PlayerReference.get(player)), "player is still in the occasion");
        helper.succeedWhen(() -> helper.assertFalse(OccasionManager.getFor(occasion.uuid()).isPresent(), "completed occasion still registered"));
    }

    @AtlantisGameTest(batch = BATCH)
    public static void aRepeatingOccasionResetsAndCanBeRejoined(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var occasion = Occasion.builder()
                .repeats()
                .recoveryPosition(position(helper, new BlockPos(1, 2, 1)))
                .newMoment().build()
                .newMoment().build()
                .build();
        occasion.addMembers(player);
        occasion.incrementMoment();

        occasion.complete();

        helper.assertValueEqual(occasion.currentMomentOrdinal(), 0, "moment after a reset");
        helper.assertValueEqual(occasion.memberCount(), 0, "members after a reset");
        helper.assertValueEqual(countMembers(occasion), 0, "members iterated after a reset");

        helper.assertValueEqual(occasion.addMembers(player), 1, "rejoining");
        helper.assertValueEqual(countMembers(occasion), 1, "members iterated after rejoining");
        helper.startSequence()
                .thenIdle(3)
                .thenExecute(() -> helper.assertTrue(OccasionManager.getFor(occasion.uuid()).isPresent(), "repeating occasion was dropped"))
                .thenExecute(occasion::removeAllMembers)
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void bossBarsShowToMembers(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var occasion = Occasion.builder()
                .recoveryPosition(position(helper, new BlockPos(1, 2, 1)))
                .newMoment().build()
                .build();
        occasion.addMembers(player);
        var moment = occasion.currentMoment();
        var shared = new AtomicReference<ServerBossEvent>();
        var individual = new AtomicReference<ServerBossEvent>();

        moment.updateBossBar(shared::set);
        moment.updateIndividualBossBar(PlayerReference.get(player), individual::set);

        helper.assertTrue(shared.get().getPlayers().contains(player), "member doesn't see the shared boss bar");
        helper.assertTrue(individual.get().getPlayers().contains(player), "member doesn't see their own boss bar");

        occasion.removeMember(player);

        helper.assertFalse(shared.get().getPlayers().contains(player), "removed member still sees the shared boss bar");
        helper.assertFalse(individual.get().getPlayers().contains(player), "removed member still sees their own boss bar");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void aMemberWhoLogsOutIsRecoveredWhenTheyLogBackIn(GameTestHelper helper) {
        var profile = new GameProfile(UUID.randomUUID(), "test-mock-player");
        var player = GameTestPlayers.mock(helper, profile);
        var recovery = position(helper, new BlockPos(1, 2, 1));
        var occasion = Occasion.builder()
                .recoveryPosition(recovery)
                .newMoment().timeout(0).build()
                .build();
        occasion.addMembers(player);
        position(helper, new BlockPos(3, 2, 3)).teleport(player);
        var rejoined = new AtomicReference<ServerPlayer>();

        GameTestPlayers.logOut(player);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(occasion.memberCount(), 0, "offline member not removed"))
                .thenExecute(() -> rejoined.set(GameTestPlayers.mock(helper, profile)))
                .thenExecute(() -> assertAt(helper, rejoined.get(), recovery))
                .thenExecute(() -> GameTestPlayers.logOut(rejoined.get()))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void aMemberRemovedWhileOnlineIsNotMovedAgainWhenTheyLogBackIn(GameTestHelper helper) {
        var profile = new GameProfile(UUID.randomUUID(), "test-mock-player");
        var player = GameTestPlayers.mock(helper, profile);
        var recovery = position(helper, new BlockPos(1, 2, 1));
        var elsewhere = position(helper, new BlockPos(3, 2, 3));
        var occasion = Occasion.builder()
                .recoveryPosition(recovery)
                .newMoment().build()
                .build();
        occasion.addMembers(player);

        occasion.removeMember(player);
        assertAt(helper, player, recovery);
        elsewhere.teleport(player);
        GameTestPlayers.logOut(player);
        var rejoined = GameTestPlayers.mock(helper, profile);

        try {
            assertAt(helper, rejoined, elsewhere);
        } finally {
            GameTestPlayers.logOut(rejoined);
        }
        helper.succeed();
    }

    private static LevelPosition position(GameTestHelper helper, BlockPos relative) {
        var absolute = helper.absolutePos(relative);
        return LevelPosition.of(ConfigurableLevel.of(helper.getLevel()), new LocalPosition(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5));
    }

    private static void assertAt(GameTestHelper helper, ServerPlayer player, LevelPosition position) {
        var target = position.position().getBlockPos();
        helper.assertTrue(player.blockPosition().equals(target), "player is at " + player.blockPosition() + ", not " + target);
    }

    private static int countMembers(Occasion occasion) {
        var count = new AtomicInteger();
        occasion.forEachMember(__ -> count.incrementAndGet());
        return count.get();
    }
}
