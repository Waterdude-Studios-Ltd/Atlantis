package de.waterdu.atlantis.util.java.collection;

import de.waterdu.atlantis.util.text.TextUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Maps keys to values of the same type. Each entry is kept as a {@link UnaryPair}, in insertion order.
 *
 * @param <T> the type of the keys and the values
 */
public class UnaryMap<T> implements Map<T, T> {

    private final Map<T, UnaryPair<T>> map;

    /**
     * Creates an empty map.
     */
    private UnaryMap() {
        this.map = new LinkedHashMap<>();
    }

    /**
     * Creates an empty map with the initial capacity.
     *
     * @param initialCapacity the initial capacity of the backing map
     */
    private UnaryMap(int initialCapacity) {
        this.map = new LinkedHashMap<>(initialCapacity);
    }

    /**
     * Creates an empty map.
     *
     * @param <T> the type of the keys and the values
     * @return the new map
     */
    public static <T> UnaryMap<T> create() {
        return new UnaryMap<>();
    }

    /**
     * Creates an empty map with the initial capacity.
     *
     * @param initialCapacity the initial capacity of the backing map
     * @param <T> the type of the keys and the values
     * @return the new map
     */
    public static <T> UnaryMap<T> create(int initialCapacity) {
        return new UnaryMap<>(initialCapacity);
    }

    /**
     * Parses text such as {@code a:1 b:2}, with a space between pairs and {@code :} between key and value.
     *
     * @param string the text to parse
     * @return the new map
     */
    public static UnaryMap<String> createFromString(String string) {
        return UnaryMap.createFromString(string, " ", ":");
    }

    /**
     * Parses text such as {@code a:1 b:2}. The delimiters are regular expressions. A key without a value maps to an
     * empty string. If a value contains the key-value delimiter, its parts are joined with spaces.
     */
    public static UnaryMap<String> createFromString(String string, String pairDelimiter, String kvDelimiter) {
        var split = string.split(pairDelimiter);
        var map = UnaryMap.<String>create(split.length);
        for (var pair : split) {
            if (pair.isBlank()) {
                continue;
            }
            var kv = pair.split(kvDelimiter);
            map.put(kv[0], kv.length == 1 ? "" : TextUtils.concat(1, " ", kv));
        }
        return map;
    }

    /**
     * @return the number of entries
     */
    @Override
    public int size() {
        return this.map.size();
    }

    /**
     * @return {@code true} if the map has no entries
     */
    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    /**
     * Checks if the map has an entry for the key.
     *
     * @param o the key
     * @return {@code true} if the key is in the map
     */
    @Override
    public boolean containsKey(Object o) {
        return this.map.containsKey(o);
    }

    /**
     * Checks if an entry has the value. It goes through all entries.
     *
     * @param o the value
     * @return {@code true} if an entry has an equal value
     */
    @Override
    public boolean containsValue(Object o) {
        for (var pair : this.map.values()) {
            if (Objects.equals(pair.b, o)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the value for the key.
     *
     * @param o the key
     * @return the value, or {@code null} if the key is not in the map
     */
    @Override
    public T get(Object o) {
        var pair = this.pair(o);
        return pair == null ? null : pair.b;
    }

    /**
     * Gets the pair of key and value for the key.
     *
     * @param o the key
     * @return the pair, or {@code null} if the key is not in the map
     */
    public UnaryPair<T> pair(Object o) {
        return this.map.get(o);
    }

    /**
     * Puts the key and value in the map as a new pair. It replaces the entry for the key, if there is one.
     *
     * @param t the key
     * @param t2 the value
     * @return the previous value, or {@code null} if the key was not in the map
     */
    @Override
    public @Nullable T put(T t, T t2) {
        var result = this.map.put(t, Pair.unary(t, t2));
        if (result == null) {
            return null;
        }
        return result.b;
    }

    /**
     * Puts the first value of the pair as the key and the second value as the value. The map stores a new pair, not
     * this one.
     *
     * @param pair the key and the value
     * @return the previous value, or {@code null} if the key was not in the map
     */
    public @Nullable T put(UnaryPair<T> pair) {
        return this.put(pair.a, pair.b);
    }

    /**
     * Removes the entry for the key.
     *
     * @param o the key
     * @return the removed value, or {@code null} if the key was not in the map
     */
    @Override
    public T remove(Object o) {
        var result = this.map.remove(o);
        if (result == null) {
            return null;
        }
        return result.b;
    }

    /**
     * Puts all entries of the other map in this map, each as a new pair.
     *
     * @param map the entries to put
     */
    @Override
    public void putAll(@NonNull Map<? extends T, ? extends T> map) {
        map.forEach((k, v) -> {
            this.map.put(k, Pair.unary(k, v));
        });
    }

    /**
     * Removes all entries.
     */
    @Override
    public void clear() {
        this.map.clear();
    }

    /**
     * @return the keys, as a live view of the backing map
     */
    @Override
    public @NonNull Set<T> keySet() {
        return this.map.keySet();
    }

    /**
     * Gets a copy of the values, in insertion order. Changes to the list do not change the map.
     *
     * @return a new list of the values
     */
    @Override
    public @NonNull Collection<T> values() {
        var list = new ArrayList<T>(this.size());
        this.map.forEach((k, v) -> list.add(v.b));
        return list;
    }

    /**
     * @return the pairs of key and value, as a live view of the backing map
     */
    public @NonNull Collection<UnaryPair<T>> pairs() {
        return this.map.values();
    }

    /**
     * Gets a copy of the entries. Changes to the set do not change the map, and the set has no fixed order.
     *
     * @return a new set of immutable entries
     */
    @Override
    public @NonNull Set<Entry<T, T>> entrySet() {
        var set = new HashSet<Entry<T, T>>(this.size());
        this.map.forEach((k, v) -> set.add(Map.entry(k, v.b)));
        return set;
    }

    /**
     * Checks if the object is a {@link Map} with equal entries.
     *
     * @param o the object to compare with
     * @return {@code true} if the entry sets are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Map<?, ?> other) {
            return this.entrySet().equals(other.entrySet());
        }
        return false;
    }

    /**
     * @return the hash code of the entry set
     */
    @Override
    public int hashCode() {
        return this.entrySet().hashCode();
    }
}
