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

/**
 * Holds elements with weights, and gets random elements by weight. An element with twice the weight is chosen twice
 * as often.
 *
 * @param <T> the type of the elements
 */
public class WeightedSet<T> implements Set<T> {

    private final TreeMap<Double, T> treeMap;
    private final Map<T, Double> weightMap;
    private double maxWeight;
    private double defaultElementWeight;

    /**
     * Creates an empty set. The default element weight is 1.
     */
    public WeightedSet() {
        this.treeMap = new TreeMap<>();
        this.weightMap = new HashMap<>();
        this.maxWeight = 0;
        this.defaultElementWeight = 1;
    }

    /**
     * Creates an empty set. The default element weight is 1.
     *
     * @param <T> the type of the elements
     * @return the new set
     */
    public static <T> WeightedSet<T> newWeightedSet() {
        return new WeightedSet<>();
    }

    /**
     * Sets the weight that {@link #add(Object)} gives to elements that do not implement {@link Weighted}.
     */
    public void setDefaultElementWeight(double weight) {
        this.defaultElementWeight = weight;
    }

    /**
     * Removes all elements and sets the total weight to 0.
     */
    @Override
    public void clear() {
        this.treeMap.clear();
        this.weightMap.clear();
        this.maxWeight = 0;
    }

    /**
     * @return a random element, chosen by weight. {@code null} if the set is empty.
     */
    public T get() {
        return this.get(RandomUtils.getRandom());
    }

    /**
     * @return a random element, chosen by weight. {@code null} if the set is empty.
     */
    public T get(Random random) {
        if (this.isEffectivelyEmpty()) {
            return null;
        } else {
            return this.treeMap.get(this.treeMap.ceilingKey(random.nextDouble() * this.maxWeight));
        }
    }

    /**
     * Gets random elements by weight and gives each one to the consumer. The same element can be chosen more than once.
     *
     * @param count the number of elements to get
     * @param consumer receives each element, or {@code null} if the set is empty
     */
    public void getAndDo(int count, Consumer<T> consumer) {
        this.getAndDo(RandomUtils.getRandom(), count, consumer);
    }

    /**
     * Gets random elements by weight and gives each one to the consumer. The same element can be chosen more than once.
     *
     * @param random the random source
     * @param count the number of elements to get
     * @param consumer receives each element, or {@code null} if the set is empty
     */
    public void getAndDo(Random random, int count, Consumer<T> consumer) {
        for (var i = 0; i < count; i++) {
            consumer.accept(this.get(random));
        }
    }

    /**
     * @return 0 if the set does not contain the element
     */
    public double getWeight(T element) {
        return this.weightMap.getOrDefault(element, 0D);
    }

    /**
     * @return the chance from 0 to 1
     */
    public double getChance(T element) {
        return this.getWeight(element) / this.maxWeight;
    }

    /**
     * @return the chance as a percentage, without the percent sign and without trailing zeros
     */
    public String getChanceString(T element, int decimalPlaces) {
        var value = String.format("%." + decimalPlaces + "f", this.getChance(element) * 100.0);
        while (value.endsWith(".0")) {
            value = value.substring(0, value.length() - 2);
        }
        if (value.contains(".")) {
            while (value.endsWith("0")) {
                value = value.substring(0, value.length() - 1);
            }
            if (value.endsWith(".")) {
                value = value.substring(0, value.length() - 1);
            }
        }
        return value;
    }

    /**
     * @return {@code true} if the set is empty or the total weight is 0 or less
     */
    public boolean isEffectivelyEmpty() {
        return this.isEmpty() || this.maxWeight <= 0;
    }

    /**
     * @return {@code true} if the set has no elements
     */
    @Override
    public boolean isEmpty() {
        return this.treeMap.isEmpty();
    }

    /**
     * Checks if the set has the element.
     *
     * @param o the element
     * @return {@code true} if the set has the element
     */
    @Override
    public boolean contains(Object o) {
        return this.weightMap.containsKey(o);
    }

    /**
     * @return the number of elements
     */
    @Override
    public int size() {
        return this.treeMap.size();
    }

    /**
     * @return the total weight of all elements
     */
    public double weight() {
        return this.maxWeight;
    }

    /**
     * @return a stream of the elements
     */
    @Override
    public @NonNull Stream<T> stream() {
        return this.treeMap.values().stream();
    }

    /**
     * Gets an iterator over the elements. Do not use it to remove elements, because the weights do not update.
     *
     * @return an iterator over the elements
     */
    @Override
    public @NonNull Iterator<T> iterator() {
        return this.treeMap.values().iterator();
    }

    /**
     * @return a spliterator over the elements
     */
    @Override
    public @NonNull Spliterator<T> spliterator() {
        return this.treeMap.values().spliterator();
    }

    /**
     * @return a new array of the elements
     */
    @Override
    public Object @NonNull [] toArray() {
        return this.treeMap.values().toArray();
    }

    /**
     * Puts the elements in the array, or in a new array if it is too small.
     *
     * @param array the array to fill
     * @param <T1> the component type of the array
     * @return the array with the elements
     */
    @Override
    public <T1> T1 @NonNull [] toArray(T1 @NonNull [] array) {
        return this.treeMap.values().toArray(array);
    }

    /**
     * Adds the element with its {@link Weighted#weight()}. If it does not implement {@link Weighted}, it gets the
     * default element weight.
     *
     * @param element the element to add
     * @return {@code false} if the weight is 0 or less, or if the set already contains the element
     */
    @Override
    public boolean add(T element) {
        if (element instanceof Weighted weighted) {
            return this.add(weighted.weight(), element);
        }
        return this.add(this.defaultElementWeight, element);
    }

    /**
     * @return {@code false} if the weight is 0 or less, or if the set already contains the element
     */
    public boolean add(double weight, T element) {
        if (weight <= 0 || this.weightMap.containsKey(element)) {
            return false;
        }
        this.maxWeight += weight;
        this.treeMap.put(this.maxWeight, element);
        this.weightMap.put(element, weight);
        return true;
    }

    /**
     * Removes the element and calculates the weights of the other elements again.
     *
     * @param element the element to remove
     * @return {@code true} if the set contained the element
     */
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

    /**
     * Checks if the set has all elements of the collection.
     *
     * @param collection the elements to check
     * @return {@code true} if the set has every element
     */
    @Override
    public boolean containsAll(Collection<?> collection) {
        for (var element : collection) {
            if (!this.weightMap.containsKey(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds each element with {@link #add(Object)}.
     *
     * @param collection the elements to add
     * @return {@code true} if the set changed
     */
    @Override
    public boolean addAll(Collection<? extends T> collection) {
        var changed = false;
        for (var element : collection) {
            changed |= this.add(element);
        }
        return changed;
    }

    /**
     * Removes each element that is not in the collection.
     *
     * @param collection the elements to keep
     * @return {@code true} if the set changed
     */
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

    /**
     * Removes each element of the collection.
     *
     * @param collection the elements to remove
     * @return {@code true} if the set changed
     */
    @Override
    public boolean removeAll(Collection<?> collection) {
        var changed = false;
        for (var element : collection) {
            changed |= this.remove(element);
        }
        return changed;
    }

    /**
     * Serializes a {@link WeightedSet} to a JSON array of its elements, and back. The weights are not stored. They come
     * from the weight function when the set is read.
     *
     * @param type the class of the elements
     * @param weightFunction gives the weight of each element that is read
     * @param <T> the type of the elements
     */
    public record TypeAdapter<T>(Class<T> type, Function<T, Double> weightFunction) implements JsonSerializer<WeightedSet<T>>, JsonDeserializer<WeightedSet<T>> {

        /**
         * Creates an adapter that uses {@link Weighted#weight()} if the type implements {@link Weighted}, otherwise a
         * weight of 1.
         *
         * @param type the class of the elements
         */
        public TypeAdapter(Class<T> type) {
            this(type, makeWeightFunction(type));
        }

        /**
         * Creates the default weight function for the type.
         *
         * @param type the class of the elements
         * @param <T> the type of the elements
         * @return {@link Weighted#weight()} if the type implements {@link Weighted}, otherwise a function that gives 1
         */
        private static <T> Function<T, Double> makeWeightFunction(Class<T> type) {
            if (Weighted.class.isAssignableFrom(type)) {
                return object -> ((Weighted) object).weight();
            } else {
                return object -> 1D;
            }
        }

        /**
         * Gets the generic type of a {@link WeightedSet} of the element class, to register the adapter with Gson.
         *
         * @param type the class of the elements
         * @return the parameterized type {@code WeightedSet<type>}
         */
        public static Type typeOf(Class<?> type) {
            return TypeToken.getParameterized(WeightedSet.class, type).getType();
        }

        /**
         * Reads a JSON array into a new set. Each element gets its weight from the weight function.
         *
         * @param json the JSON array
         * @param typeOfT the type to read
         * @param context reads each element
         * @return the new set
         */
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

        /**
         * Writes the elements of the set to a JSON array, without the weights.
         *
         * @param src the set to write
         * @param typeOfSrc the type of the set
         * @param context writes each element
         * @return the JSON array
         */
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
