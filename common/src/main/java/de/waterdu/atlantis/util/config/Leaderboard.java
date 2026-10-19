package de.waterdu.atlantis.util.config;

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

    @SuppressWarnings("unchecked")
    public Leaderboard<T, V> withRankings(Map<UUID, ? extends Comparable<?>> rankings) {
        this.rankings.putAll((Map<UUID, ? extends V>) rankings);
        return this;
    }

    @SuppressWarnings("unchecked")
    public Leaderboard<T, V> withQueued(Map<UUID, ? extends Comparable<?>> queued) {
        this.queued.putAll((Map<UUID, ? extends V>) queued);
        return this;
    }

    public static <T extends Data, V extends Comparable<V>> Leaderboard<T, V> create(
            String owner, Class<T> dataClass, String getter, Class<V> valueClass, boolean reverse
    ) throws NoSuchMethodException {
        var leaderboard = new Leaderboard<>(owner, dataClass, getter, valueClass, reverse);
        Leaderboard.ALL_LEADERBOARDS.put(leaderboard.toString(), leaderboard);
        return leaderboard;
    }

    public static <T extends Data, V extends Comparable<V>> Leaderboard<T, V> create(
            String owner, Class<T> dataClass, String getter, String arg, Class<V> valueClass, boolean reverse
    ) throws NoSuchMethodException {
        return Leaderboard.create(owner, dataClass, getter + "#" + arg, valueClass, reverse);
    }

    @SuppressWarnings("unchecked")
    private static <V extends Comparable<V>> TypeToken<Map<UUID, V>> makeMapTypeToken(Class<V> valueClass) {
        return (TypeToken<Map<UUID, V>>) TypeToken.getParameterized(Map.class, UUID.class, valueClass);
    }

    public String owner() {
        return this.owner;
    }

    public Class<T> dataClass() {
        return this.dataClass;
    }

    public Class<V> valueClass() {
        return this.valueClass;
    }

    public Map<UUID, V> rankings() {
        return ImmutableMap.copyOf(this.rankings);
    }

    public Map<UUID, V> queued() {
        return ImmutableMap.copyOf(this.queued);
    }

    public List<UUID> rankingsList() {
        return this.rankingsList;
    }

    public boolean isOnLeaderboard(UUID uuid) {
        return this.rankings.containsKey(uuid) || this.queued.containsKey(uuid);
    }

    public int positionOf(UUID uuid) {
        if (this.queued.containsKey(uuid)) {
            return this.rankings.size();
        } else {
            return this.rankingsList.indexOf(uuid);
        }
    }

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

    public CompletableFuture<List<T>> all() {
        return this.firstN(this.rankingsList.size());
    }

    public boolean isReverse() {
        return this.reverse;
    }

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

    public static void recalculateAll(boolean multithreaded) {
        if (!Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_PRE_ALL).emit(new LeaderboardEventPayload.Recalculate.PreAll<>(null)).cancelled()) {
            var future = CompletableFuture.<Void>completedFuture(null);
            for (var leaderboard : Leaderboard.ALL_LEADERBOARDS.values()) {
                future = leaderboard.recalculate(multithreaded);
            }
            future.whenComplete((v, err) -> Concurrency.sync(() -> Mods.modLoader().event(UEvent.RECALCULATE_LEADERBOARD_POST_ALL).emit(new LeaderboardEventPayload.Recalculate.PostAll<>(null))));
        }
    }

    @Override
    public int hashCode() {
        return this.stringKey.hashCode();
    }

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

    @Override
    public String toString() {
        return "Leaderboard(" + this.stringKey + ")";
    }

    public static class TypeAdapter implements JsonSerializer<Leaderboard<?, ?>>, JsonDeserializer<Leaderboard<?, ?>> {

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
