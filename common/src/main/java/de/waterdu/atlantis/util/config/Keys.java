package de.waterdu.atlantis.util.config;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Keys<T> implements Set<Key<T>> {

    private final Set<Key<T>> keys;

    private Keys() {
        this(0);
    }

    private Keys(int initialCapacity) {
        this.keys = new HashSet<>(initialCapacity);
    }

    private Keys(Collection<Key<T>> keys) {
        this.keys = new HashSet<>(keys);
    }

    public static <T> Keys<T> empty() {
        return new Keys<>();
    }

    @SafeVarargs
    public static <T> Keys<T> of(Key<T>... keys) {
        return new Keys<>(Arrays.asList(keys));
    }

    @Override
    public int size() {
        return this.keys.size();
    }

    @Override
    public boolean isEmpty() {
        return this.keys.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.keys.contains(o);
    }

    @Override
    public @NonNull Iterator<Key<T>> iterator() {
        return this.keys.iterator();
    }

    @Override
    public @NonNull Object[] toArray() {
        return this.keys.toArray();
    }

    @Override
    public @NonNull <T> T[] toArray(@NonNull T[] ts) {
        return this.keys.toArray(ts);
    }

    @Override
    public boolean add(Key<T> key) {
        return this.keys.add(key);
    }

    @Override
    public boolean remove(Object o) {
        return this.keys.remove(o);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        return this.keys.containsAll(collection);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends Key<T>> collection) {
        return this.keys.addAll(collection);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        return this.keys.retainAll(collection);
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        return this.keys.removeAll(collection);
    }

    @Override
    public void clear() {
        this.keys.clear();
    }
}
