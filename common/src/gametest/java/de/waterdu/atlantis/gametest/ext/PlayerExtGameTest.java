package de.waterdu.atlantis.gametest.ext;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Tests the methods that {@code PlayerExt} adds to players.
 */
public final class PlayerExtGameTest {

    /**
     * Prevents instantiation.
     */
    private PlayerExtGameTest() {}

    /**
     * Checks that {@code reference()} returns the interned {@link PlayerReference} of the player each time.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void referenceIsTheInternedPlayerReference(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);

        helper.assertTrue(player.reference() == PlayerReference.get(player.getUUID()), "reference() is not the interned reference");
        helper.assertTrue(player.reference() == player.reference(), "reference() is not cached");
        helper.succeed();
    }

    /**
     * Checks that {@code safeReference()} is not {@code null} and holds the value of {@code reference()}.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void safeReferenceWrapsReference(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);

        helper.assertTrue(player.safeReference() != null, "safeReference() returned null");
        helper.assertTrue(player.safeReference().orElseThrow() == player.reference(), "safeReference() is not reference()");
        helper.succeed();
    }

    /**
     * Checks that {@code getContainerCounter()} returns the value that {@code setContainerCounter} set.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void containerCounterRoundTrips(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);

        player.setContainerCounter(57);

        helper.assertValueEqual(player.getContainerCounter(), 57, "container counter");
        helper.succeed();
    }
}
