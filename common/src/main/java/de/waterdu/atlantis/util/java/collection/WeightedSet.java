package de.waterdu.atlantis.util.java.collection;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.util.java.RandomUtils;
import de.waterdu.atlantis.util.java.interfaces.Weighted;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class WeightedSet<T> implements Set<T> {

    private final TreeMap<Double, T> treeMap;
    private final Map<T, Double> weightMap;
    private double maxWeight;
    private double defaultElementWeight;

    public WeightedSet() {
        this.treeMap = new TreeMap<>();
        this.weightMap = new HashMap<>();
        this.maxWeight = 0;
        this.defaultElementWeight = 1;
    }

    public static <T> WeightedSet<T> newWeightedSet() {
        return new WeightedSet<>();
    }

    public void setDefaultElementWeight(double weight) {
        this.defaultElementWeight = weight;
    }

    @Override
    public void clear() {
        this.treeMap.clear();
        this.weightMap.clear();
        this.maxWeight = 0;
    }

    public T get() {
        return this.get(RandomUtils.getRandom());
    }

    public T get(Random random) {
        if (this.isEffectivelyEmpty()) {
            return null;
        } else {
            return this.treeMap.get(this.treeMap.ceilingKey(random.nextDouble() * this.maxWeight));
        }
    }

    public void getAndDo(int count, Consumer<T> consumer) {
        this.getAndDo(RandomUtils.getRandom(), count, consumer);
    }

    public void getAndDo(Random random, int count, Consumer<T> consumer) {
        for (var i = 0; i < count; i++) {
            consumer.accept(this.get(random));
        }
    }

    public double getWeight(T element) {
        return this.weightMap.getOrDefault(element, 0D);
    }

    public double getChance(T element) {
        return this.getWeight(element) / this.maxWeight;
    }

    public String getChanceString(T element, int decimalPlaces) {
        var value = String.format("%." + decimalPlaces + "f", this.getChance(element) * 100.0);
        while (value.endsWith(".0")) {
            value = value.substring(0, value.length() - 2);
        }
        if (value.contains(".")) {
            while (value.endsWith("0")) {
                value = value.substring(0, value.length() - 1);
            }
        }
        return value;
    }

    public boolean isEffectivelyEmpty() {
        return this.isEmpty() || this.maxWeight <= 0;
    }

    @Override
    public boolean isEmpty() {
        return this.treeMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.weightMap.containsKey(o);
    }

    @Override
    public int size() {
        return this.treeMap.size();
    }

    public double weight() {
        return this.maxWeight;
    }

    @Override
    public @NonNull Stream<T> stream() {
        return this.treeMap.values().stream();
    }

    @Override
    public @NonNull Iterator<T> iterator() {
        return this.treeMap.values().iterator();
    }

    @Override
    public @NonNull Spliterator<T> spliterator() {
        return this.treeMap.values().spliterator();
    }

    @Override
    public Object @NonNull [] toArray() {
        return this.treeMap.values().toArray();
    }

    @Override
    public <T1> T1 @NonNull [] toArray(T1 @NonNull [] array) {
        return this.treeMap.values().toArray(array);
    }

    @Override
    public boolean add(T element) {
        if (element instanceof Weighted weighted) {
            return this.add(weighted.weight(), element);
        }
        return this.add(this.defaultElementWeight, element);
    }

    public boolean add(double weight, T element) {
        if (weight <= 0 || this.weightMap.containsKey(element)) {
            return false;
        }
        this.maxWeight += weight;
        this.treeMap.put(this.maxWeight, element);
        this.weightMap.put(element, weight);
        return true;
    }

    @Override
    public boolean remove(Object element) {
        if (this.weightMap.remove(element) == null) {
            return false;
        }
        this.treeMap.clear();
        this.maxWeight = 0;
        for (var entry : this.weightMap.entrySet()) {
            this.maxWeight += entry.getValue();
            this.treeMap.put(this.maxWeight, entry.getKey());
        }
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        for (var element : collection) {
            if (!this.weightMap.containsKey(element)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        var changed = false;
        for (var element : collection) {
            changed |= this.add(element);
        }
        return changed;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        var changed = false;
        var toRemove = new HashSet<T>();
        for (var element : this.weightMap.keySet()) {
            if (!collection.contains(element)) {
                toRemove.add(element);
            }
        }
        for (var element : toRemove) {
            changed |= this.remove(element);
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        var changed = false;
        for (var element : collection) {
            changed |= this.remove(element);
        }
        return changed;
    }

    public record TypeAdapter<T>(Class<T> type, Function<T, Double> weightFunction) implements JsonSerializer<WeightedSet<T>>, JsonDeserializer<WeightedSet<T>> {

        public TypeAdapter(Class<T> type) {
            this(type, makeWeightFunction(type));
        }

        private static <T> Function<T, Double> makeWeightFunction(Class<T> type) {
            if (Weighted.class.isAssignableFrom(type)) {
                return object -> ((Weighted) object).weight();
            } else {
                return object -> 1D;
            }
        }

        public static Type typeOf(Class<?> type) {
            return TypeToken.getParameterized(WeightedSet.class, type).getType();
        }

        @Override
        public WeightedSet<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var set = WeightedSet.<T>newWeightedSet();
            var array = json.getAsJsonArray();
            for (var element : array) {
                var object = context.<T>deserialize(element, this.type);
                set.add(this.weightFunction.apply(object), object);
            }
            return set;
        }

        @Override
        public JsonElement serialize(WeightedSet<T> src, Type typeOfSrc, JsonSerializationContext context) {
            var array = new JsonArray();
            for (var object : src) {
                array.add(context.serialize(object, this.type));
            }
            return array;
        }
    }
}
