package de.waterdu.atlantis.gametest.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.gametest.GameTestConnections;
import de.waterdu.atlantis.gametest.GameTestPlayers;
import de.waterdu.atlantis.permission.Permissions;
import de.waterdu.atlantis.playerdata.AtlantisPlayer;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.concurrency.Task;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.network.NetworkUtils;
import de.waterdu.atlantis.util.server.ServerUtils;
import de.waterdu.atlantis.util.text.MessageUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ServerUtils, Concurrency, Task, PlayerReference, MessageUtils and NetworkUtils against the running server and
 * mock players. Tasks and async work go through Atlantis' shared thread pool, which player joins also use for a
 * blocking Mojang skin lookup, so those tests allow many (unthrottled) ticks.
 */
public final class ServerUtilsGameTest {

    private static final String BATCH = "atlantis_server";
    private static final int TIMEOUT = 100000;
    private static final String USER_NODE = Permissions.buildNode("atlantis_test", "command", false);
    private static final String ADMIN_NODE = Permissions.buildNode("atlantis_test", "command", true);

    private ServerUtilsGameTest() {}

    @AtlantisGameTest(batch = BATCH)
    public static void serverIsTheRunningServer(GameTestHelper helper) {
        helper.assertTrue(ServerUtils.server() == helper.getLevel().getServer(), "server() is not the running server");
        helper.assertTrue(ServerUtils.playerList() == helper.getLevel().getServer().getPlayerList(), "playerList()");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void playersListsAndFindsAMockPlayer(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var visited = new ArrayList<ServerPlayer>();

        ServerUtils.forEachPlayer(visited::add);

        helper.assertTrue(ServerUtils.players().contains(player), "players() is missing the player");
        helper.assertTrue(visited.contains(player), "forEachPlayer skipped the player");
        helper.assertTrue(ServerUtils.player(player.getUUID()).orElse(null) == player, "player(uuid)");
        helper.assertTrue(ServerUtils.player(UUID.randomUUID()).isEmpty(), "player(uuid) found an unknown uuid");
        var byName = ServerUtils.player("test-mock-player");
        helper.assertTrue(byName.isPresent(), "player(name)");
        helper.assertValueEqual(byName.get().getGameProfile().getName(), "test-mock-player", "player(name) name");
        helper.assertTrue(ServerUtils.player("atlantis-nobody").isEmpty(), "player(name) found an unknown name");
        helper.assertTrue(ServerUtils.playerNames().contains("test-mock-player"), "playerNames()");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void performCommandRunsAsTheConsole(GameTestHelper helper) throws CommandSyntaxException {
        var gameTime = (int) (helper.getLevel().getServer().overworld().getGameTime() % Integer.MAX_VALUE);

        helper.assertValueEqual(ServerUtils.performCommand("time query gametime"), gameTime, "command result");
        helper.assertValueEqual(ServerUtils.performCommand("time query %1", "gametime"), gameTime, "formatted command result");
        try {
            ServerUtils.performCommand("atlantis_test_no_such_command");
            helper.fail("an unknown command did not throw");
        } catch (CommandSyntaxException expected) {
        }
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void spawnPointIsTheOverworldSpawn(GameTestHelper helper) {
        var overworld = helper.getLevel().getServer().overworld();

        var spawn = ServerUtils.spawnPoint();

        helper.assertValueEqual(spawn.levelKey(), Level.OVERWORLD, "spawn level");
        helper.assertValueEqual(spawn.position().getBlockPos(), overworld.getSharedSpawnPos(), "spawn position");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void registriesComeFromTheServer(GameTestHelper helper) {
        var stone = ResourceLocation.withDefaultNamespace("stone");

        helper.assertTrue(ServerUtils.registryAccess() == helper.getLevel().getServer().registryAccess(), "registryAccess()");
        helper.assertTrue(ServerUtils.registry(Registries.BLOCK).get(stone) == Blocks.STONE, "registry(BLOCK)");
        helper.assertValueEqual(ServerUtils.key(Registries.BLOCK, Blocks.STONE), stone, "key(BLOCK, STONE)");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void parsePlayersResolvesSelectorsAndNames(GameTestHelper helper) throws CommandSyntaxException {
        var player = GameTestPlayers.mock(helper);
        var source = player.createCommandSourceStack().withPermission(4);
        var found = new AtomicReference<List<ServerPlayer>>();
        var failed = new AtomicInteger();

        ServerUtils.parsePlayersOrElse(source, "@s", found::set, failed::incrementAndGet);
        helper.assertTrue(List.of(player).equals(found.get()), "@s found " + found.get());
        helper.assertValueEqual(failed.get(), 0, "@s failed");

        found.set(null);
        ServerUtils.parsePlayersOrElse(source, "test-mock-player", found::set, failed::incrementAndGet);
        helper.assertTrue(found.get() != null && found.get().size() == 1, "name lookup");
        helper.assertValueEqual(found.get().getFirst().getGameProfile().getName(), "test-mock-player", "name lookup name");

        found.set(null);
        ServerUtils.parsePlayersOrElse(source, "atlantis-nobody", found::set, failed::incrementAndGet);
        ServerUtils.parsePlayersOrElse(source, "@a[name=atlantis-nobody]", found::set, failed::incrementAndGet);
        helper.assertTrue(found.get() == null, "success ran for no players");
        helper.assertValueEqual(failed.get(), 2, "failure runs");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void syncRunsOnTheServerThread(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var submittedOffThread = new AtomicReference<Boolean>();
        var ranOnServerThread = new AtomicReference<Boolean>();

        CompletableFuture.runAsync(() -> {
            submittedOffThread.set(!server.isSameThread());
            Concurrency.sync(() -> ranOnServerThread.set(server.isSameThread()));
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(Boolean.TRUE.equals(submittedOffThread.get()), "task was not submitted off-thread");
            helper.assertTrue(Boolean.TRUE.equals(ranOnServerThread.get()), "sync task ran on the server thread");
        });
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void asyncRunsOffTheServerThread(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var ranOffThread = new AtomicReference<Boolean>();

        Concurrency.async(() -> ranOffThread.set(!server.isSameThread()));

        helper.succeedWhen(() -> helper.assertTrue(Boolean.TRUE.equals(ranOffThread.get()), "async task ran off-thread"));
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void syncTaskRunsOnTheServerThreadNoEarlierThanItsDelay(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var delay = 5;
        var scheduledAt = helper.getTick();
        var ranAt = new AtomicLong(-1);
        var onServerThread = new AtomicBoolean();

        Task.builder("atlantis_test:delayed").sync().ticks(delay).task(() -> {
            onServerThread.set(server.isSameThread());
            ranAt.set(helper.getTick());
        }).schedule();

        helper.succeedWhen(() -> {
            helper.assertTrue(ranAt.get() >= 0, "task did not run");
            helper.assertTrue(onServerThread.get(), "sync task ran off the server thread");
            helper.assertTrue(ranAt.get() - scheduledAt >= delay, "task ran after " + (ranAt.get() - scheduledAt) + " ticks");
        });
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void andThenRunsAfterTheTaskOnTheServerThread(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        List<String> calls = new CopyOnWriteArrayList<>();

        Task.builder("atlantis_test:and_then").sync()
                .task(() -> calls.add("first " + server.isSameThread()))
                .andThen(it -> calls.add("second " + server.isSameThread()))
                .schedule();

        helper.succeedWhen(() -> helper.assertValueEqual(calls, List.of("first true", "second true"), "calls"));
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void repeatingSyncTaskStopsWhenCancelled(GameTestHelper helper) {
        var runs = new AtomicInteger();

        var task = Task.builder("atlantis_test:repeat_cancel").sync().repeating().milliseconds(1).task(it -> {
            if (runs.incrementAndGet() == 3) {
                it.cancel();
            }
        }).schedule();

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(task.isCancelled(), "task was not cancelled"))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertValueEqual(runs.get(), 3, "runs");
                    helper.assertFalse(task.repeats(), "cancelled task still repeats");
                })
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void repeatingSyncTaskStopsWhenRepeatIsTurnedOff(GameTestHelper helper) {
        var runs = new AtomicInteger();
        var stopped = new AtomicBoolean();

        Task.builder("atlantis_test:repeat_off").sync().repeating().milliseconds(1).task(it -> {
            if (runs.incrementAndGet() == 2) {
                it.repeat(false);
                stopped.set(true);
            }
        }).schedule();

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stopped.get(), "repeat was not turned off"))
                .thenIdle(20)
                .thenExecute(() -> helper.assertValueEqual(runs.get(), 2, "runs"))
                .thenSucceed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void cancelledTaskNeverRuns(GameTestHelper helper) {
        var runs = new AtomicInteger();
        var sentinel = new AtomicBoolean();

        var task = Task.builder("atlantis_test:cancelled").sync().milliseconds(20).task(runs::incrementAndGet).schedule();
        task.cancel();
        Task.builder("atlantis_test:cancelled_sentinel").sync().milliseconds(200).task(() -> sentinel.set(true)).schedule();

        helper.succeedWhen(() -> {
            helper.assertTrue(sentinel.get(), "sentinel did not run");
            helper.assertValueEqual(runs.get(), 0, "cancelled task runs");
        });
    }

    @AtlantisGameTest(batch = BATCH)
    public static void playerReferencesAreInterned(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);

        var reference = PlayerReference.get(player);

        helper.assertTrue(PlayerReference.get(player) == reference, "get(player) returned a new reference");
        helper.assertTrue(PlayerReference.get(player.getUUID()) == reference, "get(uuid) returned a different reference");
        helper.assertValueEqual(reference.uuid(), player.getUUID(), "uuid");
        helper.assertTrue(reference.is(player), "is(player)");
        helper.assertTrue(reference.entityDirect() == player, "entityDirect()");
        helper.assertTrue(PlayerReference.getSafely(null).isEmpty(), "getSafely(null)");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void playerReferenceResolvesTheEntityOffThread(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var reference = PlayerReference.get(player);
        reference.reset();
        var resolved = new AtomicReference<ServerPlayer>();

        Concurrency.async(() -> resolved.set(reference.entityDirect()));

        helper.succeedWhen(() -> helper.assertTrue(resolved.get() == player, "entityDirect() off-thread"));
    }

    @AtlantisGameTest(batch = BATCH)
    public static void playerReferencePermissionsMatchThePlayer(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var reference = PlayerReference.get(player);

        helper.assertTrue(reference.permission(USER_NODE), "user node");
        helper.assertFalse(reference.permission(ADMIN_NODE), "admin node");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void playerReferenceSendsChat(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        GameTestConnections.sent(player);

        PlayerReference.get(player).send("reference message");

        helper.assertTrue(receivedChat(player, "reference message"), "message was not sent");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH, timeoutTicks = TIMEOUT)
    public static void playerReferenceLoadsAndCachesAtlantisPlayerData(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var reference = PlayerReference.get(player);
        var pending = new AtomicReference<CompletableFuture<AtlantisPlayer>>();

        helper.succeedWhen(() -> {
            var future = pending.updateAndGet(it -> it == null || (it.isDone() && it.handle((data, err) -> data).getNow(null) == null) ? reference.data(AtlantisPlayer.class) : it);
            var data = future.handle((it, err) -> it).getNow(null);
            helper.assertTrue(data != null, "data not loaded");
            helper.assertValueEqual(data.uuid(), player.getUUID(), "data uuid");
            helper.assertTrue(reference.data(AtlantisPlayer.class).getNow(null) == data, "data was not cached");
            helper.assertTrue(reference.dataNow(AtlantisPlayer.class) == data, "dataNow()");
        });
    }

    @AtlantisGameTest(batch = BATCH)
    public static void playerReferenceForgetsTheEntityAfterLogout(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var reference = PlayerReference.get(player);
        helper.assertTrue(reference.entityDirect() == player, "entityDirect() before logout");

        player.connection.disconnect(Component.literal("GameTest"));

        helper.assertTrue(helper.getLevel().getServer().getPlayerList().getPlayer(player.getUUID()) == null, "player is still listed");
        helper.assertTrue(PlayerReference.get(player.getUUID()) == reference, "reference was not kept");
        helper.assertTrue(reference.entityDirect() == null, "entityDirect() returned the logged out player");
        helper.assertTrue(reference.entity().isEmpty(), "entity() is present after logout");
        helper.assertFalse(reference.permission(USER_NODE), "permission after logout");
        reference.send("after logout");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void messagesReachPlayersAndSources(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        GameTestConnections.sent(player);

        MessageUtils.send("to reference", PlayerReference.get(player));
        helper.assertTrue(receivedChat(player, "to reference"), "PlayerReference recipient");

        MessageUtils.send("to collection", List.of(PlayerReference.get(player)));
        helper.assertTrue(receivedChat(player, "to collection"), "collection recipient");

        MessageUtils.send("to source stack", player.createCommandSourceStack());
        helper.assertTrue(receivedChat(player, "to source stack"), "CommandSourceStack recipient");

        MessageUtils.send("to source", player);
        helper.assertTrue(receivedChat(player, "to source"), "CommandSource recipient");

        MessageUtils.send(Component.literal("a component"), PlayerReference.get(player));
        helper.assertTrue(receivedChat(player, "a component"), "component message");

        MessageUtils.send(null, PlayerReference.get(player));
        helper.assertFalse(GameTestConnections.sent(player).stream().anyMatch(ClientboundSystemChatPacket.class::isInstance), "null message was sent");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void filteredBroadcastsReachOnlyMatchingPlayers(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var other = GameTestPlayers.mock(helper);
        GameTestConnections.sent(player);
        GameTestConnections.sent(other);

        MessageUtils.sendToMatching("matching broadcast", it -> it == player);
        helper.assertTrue(receivedChat(player, "matching broadcast"), "sendToMatching skipped the match");
        helper.assertFalse(receivedChat(other, "matching broadcast"), "sendToMatching reached another player");

        MessageUtils.sendToAllExcept("except broadcast", it -> it != player);
        helper.assertTrue(receivedChat(player, "except broadcast"), "sendToAllExcept skipped the player");
        helper.assertFalse(receivedChat(other, "except broadcast"), "sendToAllExcept reached an excluded player");

        MessageUtils.sendToAll("global broadcast");
        helper.assertTrue(receivedChat(player, "global broadcast"), "sendToAll missed the player");
        helper.assertTrue(receivedChat(other, "global broadcast"), "sendToAll missed the other player");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void permissionBroadcastsReachOnlyPermittedPlayers(GameTestHelper helper) {
        var operator = GameTestPlayers.mock(helper);
        var player = GameTestPlayers.mock(helper);
        var list = helper.getLevel().getServer().getPlayerList();
        list.op(operator.getGameProfile());
        try {
            GameTestConnections.sent(operator);
            GameTestConnections.sent(player);

            MessageUtils.sendToAllWithPerm("admin broadcast", ADMIN_NODE);

            helper.assertTrue(receivedChat(operator, "admin broadcast"), "operator did not receive");
            helper.assertFalse(receivedChat(player, "admin broadcast"), "non-operator received");
        } finally {
            list.deop(operator.getGameProfile());
        }
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void loginRecordsThePlayersAddress(GameTestHelper helper) {
        var player = GameTestPlayers.mock(helper);
        var reference = PlayerReference.get(player);

        Optional<SocketAddress> address = reference.address();
        var ip = reference.ip();

        helper.assertTrue(address.isPresent(), "address was not recorded");
        helper.assertTrue(ip.isPresent(), "ip was not recorded");
        helper.assertTrue(java.util.Objects.equals(ip.get(), NetworkUtils.addressToIP(address.get())), "ip was " + ip.get());
        helper.assertTrue(NetworkUtils.player(address.get()).contains(reference), "player(address)");
        helper.assertTrue(NetworkUtils.player(ip.get()).contains(reference), "player(ip)");

        NetworkUtils.addAddress(player);
        helper.assertValueEqual(reference.ip(), ip, "ip after adding again");
        helper.succeed();
    }

    private static boolean receivedChat(ServerPlayer player, String text) {
        return GameTestConnections.sent(player).stream()
                .anyMatch(packet -> packet instanceof ClientboundSystemChatPacket chat && !chat.overlay() && chat.content().getString().equals(text));
    }
}
