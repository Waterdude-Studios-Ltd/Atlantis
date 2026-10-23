package de.waterdu.atlantis.gametest.file;

import com.mojang.authlib.GameProfile;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.playerdata.AtlantisPlayer;
import de.waterdu.atlantis.playerdata.UUIDNameCache;
import net.minecraft.Util;
import net.minecraft.gametest.framework.GameTestHelper;

import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atlantis' own player data on join and leave, and the name cache. Each test uses its own player names, because
 * every default mock player is called "test-mock-player".
 */
public final class PlayerDataGameTest {

    private static final String BATCH = "atlantis_player_data";
    private static final int TIMEOUT = 100000;

    private PlayerDataGameTest() {}

    @AtlantisGameTest(batch = BATCH)
    public static void joiningRecordsTheName(GameTestHelper helper) {
        var profile = profile();

        var player = GameTestPlayers.mock(helper, profile);

        try {
            helper.assertValueEqual(UUIDNameCache.get(profile.getName()), profile.getId(), "uuid of the name");
            helper.assertValueEqual(UUIDNameCache.get(profile.getId()), profile.getName(), "name of the uuid");
        } finally {
            GameTestPlayers.logOut(player);
        }
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void aNameTakenOverByAnotherPlayerMovesToThem(GameTestHelper helper) {
        var name = name();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();

        UUIDNameCache.add(name, first);
        UUIDNameCache.add(name, second);

        helper.assertValueEqual(UUIDNameCache.get(name), second, "uuid of the name");
        helper.assertValueEqual(UUIDNameCache.get(second), name, "name of the new owner");
        helper.assertValueEqual(UUIDNameCache.get(first), "", "name of the old owner");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void aRenamedPlayerKeepsOnlyTheNewName(GameTestHelper helper) {
        var uuid = UUID.randomUUID();
        var oldName = name();
        var newName = name();

        UUIDNameCache.add(oldName, uuid);
        UUIDNameCache.add(newName, uuid);

        helper.assertValueEqual(UUIDNameCache.get(uuid), newName, "name of the uuid");
        helper.assertValueEqual(UUIDNameCache.get(newName), uuid, "uuid of the new name");
        helper.assertValueEqual(UUIDNameCache.get(oldName), Util.NIL_UUID, "uuid of the old name");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void joiningCreatesAtlantisPlayerData(GameTestHelper helper) {
        var profile = profile();
        var player = GameTestPlayers.mock(helper, profile);
        var data = new AtomicReference<AtlantisPlayer>();
        AtlantisData.get(player).thenAccept(data::set);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(data.get() != null, "no player data"))
                .thenExecute(() -> {
                    helper.assertValueEqual(data.get().uuid(), profile.getId(), "uuid");
                    helper.assertValueEqual(data.get().name(), profile.getName(), "name");
                    helper.assertTrue(data.get().lastLogin() > 0, "last login not recorded");
                })
                .thenExecute(() -> GameTestPlayers.logOut(player))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void leavingSavesThePlayerData(GameTestHelper helper) {
        var profile = profile();
        var player = GameTestPlayers.mock(helper, profile);
        var file = Mods.modLoader().configDir().resolve(Atlantis.ID).resolve("players").resolve(profile.getId() + ".json");
        var data = new AtomicReference<AtlantisPlayer>();
        AtlantisData.get(player).thenAccept(data::set);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(data.get() != null, "no player data"))
                .thenExecute(() -> GameTestPlayers.logOut(player))
                .thenWaitUntil(() -> helper.assertTrue(Files.exists(file), "player data not saved to " + file))
                .thenSucceed();
    }

    private static GameProfile profile() {
        return new GameProfile(UUID.randomUUID(), name());
    }

    private static String name() {
        return "gt" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
