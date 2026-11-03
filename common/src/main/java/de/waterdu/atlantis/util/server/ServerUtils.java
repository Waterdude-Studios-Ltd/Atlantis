package de.waterdu.atlantis.util.server;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.mod.Mods;
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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Gets the running server, its registries and its players.
 */
public final class ServerUtils {

    private static final List<ServerPlayer> NO_PLAYERS = List.of();
    private static final String[] NO_NAMES = new String[0];
    private static final LevelPosition BEST_GUESS_SPAWN_POINT = LevelPosition.of("minecraft:overworld", 0, 64, 0);

    /**
     * Always throws, because this class only has static methods.
     */
    private ServerUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * @return the running server from the mod loader, or {@code null} when no server runs
     */
    //noinspection resource
    public static @Nullable MinecraftServer server() {
        return Mods.modLoader().server();
    }

    /**
     * Gets a value from the running server.
     *
     * @param <T> the type of the value
     * @param getter gets the value from the server
     * @return the value, or {@code null} when no server runs
     */
    public static <T> @Nullable T fromServer(Function<@NonNull MinecraftServer, @Nullable T> getter) {
        var server = ServerUtils.server();
        if (server == null) {
            return null;
        }
        return getter.apply(server);
    }

    /**
     * Gets a value from the running server, or a fallback value.
     *
     * @param <T> the type of the value
     * @param getter gets the value from the server
     * @param orElse the value to return when no server runs or the getter returns {@code null}
     * @return the value, or {@code orElse}
     */
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

    /**
     * @return {@link RegistryAccess#EMPTY} when no server runs
     */
    public static @NonNull RegistryAccess registryAccess() {
        return ServerUtils.fromServer(MinecraftServer::registryAccess, RegistryAccess.EMPTY);
    }

    /**
     * Throws an exception when no server runs or the registry does not exist.
     */
    public static <T> Registry<T> registry(ResourceKey<Registry<T>> key) {
        return ServerUtils.registryAccess().registryOrThrow(key);
    }

    /**
     * Gets the key of an entry in a registry of the server. Throws an exception when no server runs or the registry
     * does not exist.
     *
     * @param <T> the type of the registry entries
     * @param key the key of the registry
     * @param instance the entry to get the key of
     * @return the key of the entry, or {@code null} if the registry does not contain it
     */
    public static <T> ResourceLocation key(ResourceKey<Registry<T>> key, T instance) {
        return ServerUtils.registry(key).getKey(instance);
    }

    /**
     * @return the player list of the server, or {@code null} when no server runs
     */
    public static @Nullable PlayerList playerList() {
        return ServerUtils.fromServer(MinecraftServer::getPlayerList);
    }

    /**
     * @return the online players, or an empty list when no server runs
     */
    public static @NonNull List<ServerPlayer> players() {
        var list = ServerUtils.playerList();
        if (list == null) {
            return NO_PLAYERS;
        }
        return list.getPlayers();
    }

    /**
     * Runs the action for each online player.
     *
     * @param forEach the action to run
     */
    public static void forEachPlayer(Consumer<ServerPlayer> forEach) {
        for (ServerPlayer player : ServerUtils.players()) {
            forEach.accept(player);
        }
    }

    /**
     * Gets an online player by UUID.
     *
     * @param uuid the UUID of the player
     * @return the player, or empty if the player is offline or no server runs
     */
    public static Optional<ServerPlayer> player(UUID uuid) {
        var list = ServerUtils.playerList();
        if (list == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.getPlayer(uuid));
    }

    /**
     * Gets an online player by name.
     *
     * @param name the name of the player
     * @return the player, or empty if the player is offline or no server runs
     */
    public static Optional<ServerPlayer> player(String name) {
        var list = ServerUtils.playerList();
        if (list == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(list.getPlayerByName(name));
    }

    /**
     * @param getterString an entity selector if it contains {@code @}. Otherwise, the name of an online player.
     * @param success gets the players if at least one matches
     * @param failure runs if no player matches
     */
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

    /**
     * @return a new list of the names of the online players. Empty when no server runs.
     */
    public static List<String> playerNames() {
        return Lists.newArrayList(ServerUtils.fromServer(MinecraftServer::getPlayerNames, NO_NAMES));
    }

    /**
     * Runs the command as the server.
     *
     * @param command the command without a leading slash. It can have the placeholders {@code %1}, {@code %2} and so
     *     on.
     * @param args the values for the placeholders
     * @return the result of the command. 0 when no server runs.
     */
    public static int performCommand(String command, Object... args) throws CommandSyntaxException {
        var server = ServerUtils.server();
        if (server == null) {
            return 0;
        }
        var dispatcher = server.getFunctions().getDispatcher();
        return dispatcher.execute(dispatcher.parse(TextUtils.format(command, args).get(), server.createCommandSourceStack()));
    }

    /**
     * @return the spawn point of the overworld. 0, 64, 0 in the overworld when no server runs.
     */
    public static LevelPosition spawnPoint() {
        var overworld = ServerUtils.fromServer(MinecraftServer::overworld);
        if (overworld == null) {
            return BEST_GUESS_SPAWN_POINT;
        }
        return LevelPosition.of(ConfigurableLevel.of(overworld), new LocalPosition(overworld.getSharedSpawnPos()));
    }
}
