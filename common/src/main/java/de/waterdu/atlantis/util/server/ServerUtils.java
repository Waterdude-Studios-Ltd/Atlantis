package de.waterdu.atlantis.util.server;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.level.LocalPosition;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ServerUtils {

    private static final List<ServerPlayer> NO_PLAYERS = List.of();
    private static final String[] NO_NAMES = new String[0];
    private static final LevelPosition BEST_GUESS_SPAWN_POINT = LevelPosition.of("minecraft:overworld", 0, 64, 0);

    private ServerUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    //noinspection resource
    public static @Nullable MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static <T> @Nullable T fromServer(Function<@NonNull MinecraftServer, @Nullable T> getter) {
        var server = ServerUtils.server();
        if (server == null) {
            return null;
        }
        return getter.apply(server);
    }

    public static <T> @NonNull T fromServer(Function<@NonNull MinecraftServer, @Nullable T> getter, @NonNull T orElse) {
        var server = ServerUtils.server();
        if (server == null) {
            return orElse;
        }
        var result = getter.apply(server);
        if (result == null) {
            return orElse;
        }
        return result;
    }

    public static @NonNull RegistryAccess registryAccess() {
        return ServerUtils.fromServer(MinecraftServer::registryAccess, RegistryAccess.EMPTY);
    }

    public static <T> Registry<T> registry(ResourceKey<Registry<T>> key) {
        return ServerUtils.registryAccess().registryOrThrow(key);
    }

    public static <T> ResourceLocation key(ResourceKey<Registry<T>> key, T instance) {
        return ServerUtils.registry(key).getKey(instance);
    }

    public static @Nullable PlayerList playerList() {
        return ServerUtils.fromServer(MinecraftServer::getPlayerList);
    }

    public static @NonNull List<ServerPlayer> players() {
        var list = ServerUtils.playerList();
        if (list == null) {
            return NO_PLAYERS;
        }
        return list.getPlayers();
    }

    public static void forEachPlayer(Consumer<ServerPlayer> forEach) {
        for (ServerPlayer player : ServerUtils.players()) {
            forEach.accept(player);
        }
    }

    public static Optional<ServerPlayer> player(UUID uuid) {
        var list = ServerUtils.playerList();
        if (list == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.getPlayer(uuid));
    }

    public static Optional<ServerPlayer> player(String name) {
        var list = ServerUtils.playerList();
        if (list == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.getPlayerByName(name));
    }

    public static void parsePlayersOrElse(CommandSourceStack source, String getterString, Consumer<List<ServerPlayer>> success, Runnable failure) throws CommandSyntaxException {
        List<ServerPlayer> players;
        if (getterString.contains("@")) {
            players = new EntitySelectorParser(new StringReader(getterString), true).parse().findPlayers(source);
        } else {
            players = new ArrayList<>();
            var list = ServerUtils.playerList();
            if (list != null) {
                var target = list.getPlayerByName(getterString);
                if (target != null) {
                    players.add(target);
                }
            }
        }
        if (!players.isEmpty()) {
            success.accept(players);
        } else {
            failure.run();
        }
    }

    public static List<String> playerNames() {
        return Lists.newArrayList(ServerUtils.fromServer(MinecraftServer::getPlayerNames, NO_NAMES));
    }

    public static int performCommand(String command, Object... args) throws CommandSyntaxException {
        var server = ServerUtils.server();
        if (server == null) {
            return 0;
        }
        var dispatcher = server.getFunctions().getDispatcher();
        return dispatcher.execute(dispatcher.parse(TextUtils.format(command, args).get(), server.createCommandSourceStack()));
    }

    public static LevelPosition spawnPoint() {
        var overworld = ServerUtils.fromServer(MinecraftServer::overworld);
        if (overworld == null) {
            return BEST_GUESS_SPAWN_POINT;
        }
        return LevelPosition.of(ConfigurableLevel.of(overworld), new LocalPosition(overworld.getSharedSpawnPos()));
    }
}
