package de.waterdu.atlantis.gametest.event;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.mixin.network.MixinServerCommonPacketListenerImpl;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests the {@link UEvent#LOG_IN} and {@link UEvent#LOG_OUT} events.
 */
public final class PlayerConnectionGameTest {

    private static final Set<Player> LOGGED_IN = ConcurrentHashMap.newKeySet();
    private static final Set<Player> LOGGED_OUT = ConcurrentHashMap.newKeySet();
    private static boolean listening = false;

    /**
     * Prevents instantiation.
     */
    private PlayerConnectionGameTest() {}

    /**
     * Checks that the log in event fires when a player joins, and that the log out event does not.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void loginFiresWhenAPlayerJoins(GameTestHelper helper) {
        listen();

        var player = GameTestPlayers.mock(helper);

        helper.assertTrue(LOGGED_IN.contains(player), "LOG_IN did not fire");
        helper.assertFalse(LOGGED_OUT.contains(player), "LOG_OUT fired on join");
        helper.succeed();
    }

    /**
     * Checks that the log out event fires when a player disconnects and that the server removes the player from its
     * player list.
     *
     * @param helper the helper of the running test
     */
    @AtlantisGameTest
    public static void logoutFiresWhenAPlayerDisconnects(GameTestHelper helper) {
        listen();
        var player = GameTestPlayers.mock(helper);

        var connection = ((MixinServerCommonPacketListenerImpl) (Object) player.connection).atlantis$getConnection();
        connection.disconnect(Component.literal("GameTest"));
        connection.handleDisconnection();

        helper.assertTrue(LOGGED_OUT.contains(player), "LOG_OUT did not fire");
        helper.assertTrue(helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == null, "player is still listed");
        helper.succeed();
    }

    /**
     * Listens for the log in and log out events and records their players. Calls after the first do nothing.
     */
    private static synchronized void listen() {
        if (listening) {
            return;
        }
        listening = true;
        Mods.modLoader().event(UEvent.LOG_IN).listen(payload -> {
            LOGGED_IN.add(payload.player());
            return InteractionResult.PASS;
        });
        Mods.modLoader().event(UEvent.LOG_OUT).listen(payload -> {
            LOGGED_OUT.add(payload.player());
            return InteractionResult.PASS;
        });
    }
}
