package de.waterdu.atlantis.util.java.collection;

import de.waterdu.atlantis.util.text.TextUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class UnaryMap<T> implements Map<T, T> {

    private final Map<T, UnaryPair<T>> map;

    private UnaryMap() {
        this.map = new LinkedHashMap<>();
    }

    private UnaryMap(int initialCapacity) {
        this.map = new LinkedHashMap<>(initialCapacity);
    }

    public static <T> UnaryMap<T> create() {
        return new UnaryMap<>();
    }

    public static <T> UnaryMap<T> create(int initialCapacity) {
        return new UnaryMap<>(initialCapacity);
    }

    public static UnaryMap<String> createFromString(String string) {
        return UnaryMap.createFromString(string, " ", ":");
    }

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

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return this.map.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return this.map.containsValue(o);
    }

    @Override
    public T get(Object o) {
        return this.pair(o).b;
    }

    public UnaryPair<T> pair(Object o) {
        return this.map.get(o);
    }

    @Override
    public @Nullable T put(T t, T t2) {
        var result = this.map.put(t, Pair.unary(t, t2));
        if (result == null) {
            return null;
        }
        return result.b;
    }

    public @Nullable T put(UnaryPair<T> pair) {
        return this.put(pair.a, pair.b);
    }

    @Override
    public T remove(Object o) {
        var result = this.map.remove(o);
        if (result == null) {
            return null;
        }
        return result.b;
    }

    @Override
    public void putAll(@NonNull Map<? extends T, ? extends T> map) {
        map.forEach((k, v) -> {
            this.map.put(k, Pair.unary(k, v));
        });
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public @NonNull Set<T> keySet() {
        return this.map.keySet();
    }

    @Override
    public @NonNull Collection<T> values() {
        var list = new ArrayList<T>(this.size());
        this.map.forEach((k, v) -> list.add(v.b));
        return list;
    }

    public @NonNull Collection<UnaryPair<T>> pairs() {
        return this.map.values();
    }

    @Override
    public @NonNull Set<Entry<T, T>> entrySet() {
        var set = new HashSet<Entry<T, T>>(this.size());
        this.map.forEach((k, v) -> set.add(Map.entry(k, v.b)));
        return set;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof UnaryMap<?> other) {
            return this.map.equals(other.map);
        }
        return this.map.equals(o);
    }
}
