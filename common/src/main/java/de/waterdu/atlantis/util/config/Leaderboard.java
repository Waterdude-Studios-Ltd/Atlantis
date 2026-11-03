package de.waterdu.atlantis.util.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.LeaderboardEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.text.TextUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Ranks players by a value that a getter reads from their {@link Data}. New players are queued until the next
 * {@link #recalculate(boolean)}.
 *
 * @param <T> the type of the player data
 * @param <V> the type of the ranked value
 */
public class Leaderboard<T extends Data, V extends Comparable<V>> {

    protected static final Map<String, Leaderboard<?, ?>> ALL_LEADERBOARDS = new ConcurrentHashMap<>();

    private final String owner;
    private final String[] getterName;
    private final Method getter;
    private final Class<T> dataClass;
    private final String dataClassName;
    private final Class<V> valueClass;
    private final String valueClassName;
    private final boolean reverse;
    private final Map<UUID, V> rankings;
    private final Map<UUID, V> queued;

    private final List<UUID> rankingsList = Lists.newCopyOnWriteArrayList();

    private final TypeToken<Map<UUID, V>> mapTypeToken;

    private final String stringKey;

    /**
     * Creates an empty leaderboard. It is not registered.
     *
     * @param owner the ID of the mod that owns the data
     * @param dataClass the class of the player data
     * @param getter the name of the getter, or {@code name#arg} for a getter that takes one {@link String}
     * @param valueClass the class of the ranked value
     * @param reverse if {@code true}, the highest value ranks first
     * @throws NoSuchMethodException if {@code dataClass} does not declare the getter
     */
    private Leaderboard(String owner, Class<T> dataClass, String getter, Class<V> valueClass, boolean reverse) throws NoSuchMethodException {
        this.owner = owner;
        this.dataClass = dataClass;
        this.dataClassName = dataClass.getName();
        this.getterName = getter.split("#");
        if (this.getterName.length == 1) {
            this.getter = dataClass.getDeclaredMethod(this.getterName[0]);
        } else {
            this.getter = dataClass.getDeclaredMethod(this.getterName[0], String.class);
        }
        this.valueClass = valueClass;
        this.valueClassName = valueClass.getName();
        this.reverse = reverse;
        this.rankings = new ConcurrentHashMap<>();
        this.queued = new ConcurrentHashMap<>();
        this.mapTypeToken = Leaderboard.makeMapTypeToken(this.valueClass);
        this.stringKey = "Leaderboard{" + this.owner + "," + this.dataClass.getSimpleName() + "," + Arrays.toString(this.getterName) + "," + this.valueClass.getSimpleName() + "}";
    }

    /**
     * Removes all registered leaderboards.
     */
    @VisibleForTesting
    public static void resetForTesting() {
        ALL_LEADERBOARDS.clear();
    }

    /**
     * Adds ranked values to the leaderboard. The values must be of the value class. The rank order changes only at the
     * next {@link #recalculate(boolean)}.
     *
     * @param rankings the ranked values of the players
     * @return this leaderboard
     */
    @SuppressWarnings("unchecked")
    public Leaderboard<T, V> withRankings(Map<UUID, ? extends Comparable<?>> rankings) {
        this.rankings.putAll((Map<UUID, ? extends V>) rankings);
        return this;
    }

    /**
     * Adds queued values to the leaderboard. The values must be of the value class.
     *
     * @param queued the queued values of the players
     * @return this leaderboard
     */
    @SuppressWarnings("unchecked")
    public Leaderboard<T, V> withQueued(Map<UUID, ? extends Comparable<?>> queued) {
        this.queued.putAll((Map<UUID, ? extends V>) queued);
        return this;
    }

    /**
     * Creates a leaderboard and registers it for {@link #recalculateAll(boolean)}. It replaces a registered leaderboard
     * with the same owner, classes and getter. Throws {@link NoSuchMethodException} if {@code dataClass} does not
     * declare the getter.
     *
     * @param owner the ID of the mod that owns the data
     * @param getter the name of a method with no parameters that {@code dataClass} declares. Inherited methods are not
     *     found. Use {@code name#arg} for a method that takes one {@link String}.
     * @param reverse if {@code true}, the highest value ranks first. If {@code false}, the lowest value ranks first.
     */
    public static <T extends Data, V extends Comparable<V>> Leaderboard<T, V> create(
            String owner, Class<T> dataClass, String getter, Class<V> valueClass, boolean reverse
    ) throws NoSuchMethodException {
        var leaderboard = new Leaderboard<>(owner, dataClass, getter, valueClass, reverse);
        Leaderboard.ALL_LEADERBOARDS.put(leaderboard.toString(), leaderboard);
        return leaderboard;
    }

    /**
     * @param arg the {@link String} argument for the getter
     */
    public static <T extends Data, V extends Comparable<V>> Leaderboard<T, V> create(
            String owner, Class<T> dataClass, String getter, String arg, Class<V> valueClass, boolean reverse
    ) throws NoSuchMethodException {
        return Leaderboard.create(owner, dataClass, getter + "#" + arg, valueClass, reverse);
    }

    /**
     * Creates the type token of a map from player UUIDs to values.
     *
     * @param valueClass the class of the values
     * @param <V> the type of the values
     * @return the type token of {@code Map<UUID, V>}
     */
    @SuppressWarnings("unchecked")
    private static <V extends Comparable<V>> TypeToken<Map<UUID, V>> makeMapTypeToken(Class<V> valueClass) {
        return (TypeToken<Map<UUID, V>>) TypeToken.getParameterized(Map.class, UUID.class, valueClass);
    }

    /**
     * @return the ID of the mod that owns the data
     */
    public String owner() {
        return this.owner;
    }

    /**
     * @return the class of the player data
     */
    public Class<T> dataClass() {
        return this.dataClass;
    }

    /**
     * @return the class of the ranked value
     */
    public Class<V> valueClass() {
        return this.valueClass;
    }

    /**
     * @return a copy of the ranked values. The copy is not in rank order.
     */
    public Map<UUID, V> rankings() {
        return ImmutableMap.copyOf(this.rankings);
    }

    /**
     * @return a copy of the values that are not ranked yet. The next {@link #recalculate(boolean)} ranks them.
     */
    public Map<UUID, V> queued() {
        return ImmutableMap.copyOf(this.queued);
    }

    /**
     * @return the live list of ranked players, in rank order. Do not modify it.
     */
    public List<UUID> rankingsList() {
        return this.rankingsList;
    }

    /**
     * Checks if the player is ranked or queued.
     *
     * @param uuid the UUID of the player
     * @return {@code true} if the player is ranked or queued
     */
    public boolean isOnLeaderboard(UUID uuid) {
        return this.rankings.containsKey(uuid) || this.queued.containsKey(uuid);
    }

    /**
     * @return the 0-based position. A queued player is after the last ranked player. -1 if the player is not on the
     *     leaderboard.
     */
    public int positionOf(UUID uuid) {
        if (this.queued.containsKey(uuid)) {
            return this.rankings.size();
        } else {
            return this.rankingsList.indexOf(uuid);
        }
    }

    /**
     * Loads the data of the first {@code n} ranked players on the Atlantis thread pool. Players whose data does not
     * load before the timeout are left out.
     */
    public CompletableFuture<List<T>> firstN(int n) {
        var future = new CompletableFuture<List<T>>();
        Atlantis.THREAD_POOL.submit(() -> {
            var timeout = Settings.get().getLeaderboardGetDataTimeoutSeconds();
            var rankingsCopy = this.rankingsList();
            var config = AtlantisConfigProxy.of(this.owner());
            var list = new ArrayList<T>();
            for (var i = 0; i < Math.min(rankingsCopy.size(), n); i++) {
                var uuid = rankingsCopy.get(i);
                try {
                    list.add(config.get(this.dataClass(), uuid).get(timeout, TimeUnit.SECONDS));
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    AtlantisLogger.warn("Failed to fetch data when getting for leaderboard: " + uuid);
                }
            }
            future.complete(list);
        });
        return future;
    }

    /**
     * Loads the data of a ranked player on the Atlantis thread pool.
     *
     * @param n the 1-based rank
     * @return completes with an {@link IndexOutOfBoundsException} if fewer than {@code n} players are ranked
     */
    public CompletableFuture<T> get(int n) {
        var future = new CompletableFuture<T>();
        Atlantis.THREAD_POOL.submit(() -> {
            var timeout = Settings.get().getLeaderboardGetDataTimeoutSeconds();
            var rankingsCopy = this.rankingsList();
            if (rankingsCopy.size() < n) {
                future.completeExceptionally(new IndexOutOfBoundsException());
            } else {
                var uuid = rankingsCopy.get(n - 1);
                var config = AtlantisConfigProxy.of(this.owner());
                try {
                    future.complete(config.get(this.dataClass(), uuid).get(timeout, TimeUnit.SECONDS));
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    AtlantisLogger.warn("Failed to fetch data when getting for leaderboard: " + uuid);
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    /**
     * Loads the data of all ranked players on the Atlantis thread pool, with {@link #firstN(int)}.
     *
     * @return completes with the data in rank order
     */
    public CompletableFuture<List<T>> all() {
        return this.firstN(this.rankingsList.size());
    }

    /**
     * @return {@code true} if the highest value ranks first
     */
    public boolean isReverse() {
        return this.reverse;
    }

    /**
     * Reads the value from the data. A player that is not ranked yet is queued. The rank order changes only at the next
     * {@link #recalculate(boolean)}.
     *
     * @return {@code false} if the getter fails. {@code true} also if an event listener cancels the update.
     */
    @SuppressWarnings("unchecked")
    public boolean update(T data) {
        try {
            UUID key = data.uuid();
            V value;
            if (this.getterName.length == 1) {
                value = (V) this.getter.invoke(data);
            } else {
                value = (V) this.getter.invoke(data, this.getterName[1]);
            }
            if (!Mods.modLoader().event(UEvent.UPDATE_LEADERBOARD).emit(new LeaderboardEventPayload.Update<>(this, data, key, value)).cancelled()) {
                if (this.rankings.containsKey(key)) {
                    this.rankings.put(key, value);
                } else {
                    this.queued.put(key, value);
                }
            }
            return true;
        } catch (InvocationTargetException | IllegalAccessException e) {
            AtlantisLogger.error("Failed to update leaderboard data: {}, {}, {}", this.stringKey, data.uuid(), e.getMessage());
            return false;
        }
    }

    /**
     * Sorts the ranked and queued players again.
     *
     * @param async if {@code true}, runs on the Atlantis thread pool
     * @return completes when the work is done, also if an event listener cancels it
     */
    public CompletableFuture<Void> recalculate(boolean async) {
        var future = new CompletableFuture<Void>();
        Runnable recalculateTask = () -> {
            if (!Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_PRE).emit(new LeaderboardEventPayload.Recalculate.Pre<>(this)).cancelled()) {
                var entries = new ArrayList<>(this.queued.entrySet());
                entries.addAll(this.rankings.entrySet());
                this.queued.clear();
                var comparator = Map.Entry.<UUID, V>comparingByValue();
                if (this.reverse) {
                    comparator = comparator.reversed();
                }
                entries.sort(comparator);
                var newRanking = new LinkedHashMap<UUID, V>();
                for (var entry : entries) {
                    newRanking.put(entry.getKey(), entry.getValue());
                }
                this.rankings.clear();
                this.rankings.putAll(newRanking);
                this.rankingsList.clear();
                this.rankingsList.addAll(newRanking.keySet());
                Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_POST).emit(new LeaderboardEventPayload.Recalculate.Post<>(this));
            }
            future.complete(null);
        };
        if (async) {
            Atlantis.THREAD_POOL.submit(recalculateTask);
        } else {
            recalculateTask.run();
        }
        return future;
    }

    /**
     * Recalculates every registered leaderboard. The post-all event fires on the server thread after all of them
     * complete.
     *
     * @param multithreaded if {@code true}, each leaderboard recalculates on the Atlantis thread pool
     */
    public static void recalculateAll(boolean multithreaded) {
        if (!Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_PRE_ALL).emit(new LeaderboardEventPayload.Recalculate.PreAll<>(null)).cancelled()) {
            var futures = Leaderboard.ALL_LEADERBOARDS.values().stream().map(leaderboard -> leaderboard.recalculate(multithreaded)).toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).whenComplete((v, err) -> Concurrency.sync(() -> Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_POST_ALL).emit(new LeaderboardEventPayload.Recalculate.PostAll<>(null))));
        }
    }

    /**
     * @return the hash code of the owner, classes and getter of the leaderboard
     */
    @Override
    public int hashCode() {
        return this.stringKey.hashCode();
    }

    /**
     * Checks if the other object is a leaderboard with the same owner, classes and getter. The values are ignored.
     *
     * @param obj the object to compare with
     * @return {@code true} if the leaderboards are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Leaderboard)) {
            return false;
        }
        return this.stringKey.equals(((Leaderboard<?, ?>) obj).stringKey);
    }

    /**
     * @return the owner, classes and getter of the leaderboard. It is also the key in the registry.
     */
    @Override
    public String toString() {
        return "Leaderboard(" + this.stringKey + ")";
    }

    /**
     * Serializes a leaderboard to a JSON object and deserializes it from one, with its ranked and queued values.
     */
    public static class TypeAdapter implements JsonSerializer<Leaderboard<?, ?>>, JsonDeserializer<Leaderboard<?, ?>> {

        /**
         * Creates and registers a leaderboard from the JSON object, then recalculates it on this thread.
         *
         * @param json the JSON object
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context, which reads the value maps
         * @return the leaderboard
         * @throws JsonParseException if a class or the getter is not found, or the object is not valid
         */
        @SuppressWarnings({"unchecked", "RawUseOfParameterized"})
        @Override
        public Leaderboard<?, ?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var object = json.getAsJsonObject();

            var owner = object.get("owner").getAsString();
            var getter = object.get("getter").getAsString();
            try {
                var dataClass = (Class<? extends Data>) Class.forName(object.get("dataClass").getAsString());
                var valueClass = (Class<? extends Comparable>) Class.forName(object.get("valueClass").getAsString());
                var reverse = object.get("reverse").getAsBoolean();

                var mapTypeToken = TypeToken.getParameterized(Map.class, UUID.class, valueClass);
                var mapType = mapTypeToken.getType();

                var rankings = context.<Map<UUID, ? extends Comparable<?>>>deserialize(object.get("rankings"), mapType);
                var queued = context.<Map<UUID, ? extends Comparable<?>>>deserialize(object.get("queued"), mapType);

                var leaderboard = Leaderboard.create(owner, dataClass, getter, valueClass, reverse).withRankings(rankings).withQueued(queued);
                leaderboard.recalculate(false);
                return leaderboard;
            } catch (Exception e) {
                throw new JsonParseException("Failed to parse leaderboard with owner " + owner + " and getter " + getter);
            }
        }

        /**
         * Writes the owner, getter, class names, order and the ranked and queued values to a JSON object.
         *
         * @param src the leaderboard to serialize
         * @param typeOfSrc the type of the leaderboard
         * @param context the serialization context, which writes the value maps
         * @return the JSON object
         */
        @Override
        public JsonElement serialize(Leaderboard<?, ?> src, Type typeOfSrc, JsonSerializationContext context) {
            var object = new JsonObject();

            object.addProperty("owner", src.owner);
            object.addProperty("getter", TextUtils.concat(0, "#", src.getterName));
            object.addProperty("dataClass", src.dataClassName);
            object.addProperty("valueClass", src.valueClassName);
            object.addProperty("reverse", src.reverse);
            object.add("rankings", context.serialize(src.rankings, src.mapTypeToken.getType()));
            object.add("queued", context.serialize(src.queued, src.mapTypeToken.getType()));

            return object;
        }
    }
}
