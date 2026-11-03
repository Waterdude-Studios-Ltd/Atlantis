package de.waterdu.atlantis.util.config;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Holds a set of {@link Key}s of one value type. It delegates every operation to a {@link HashSet}.
 *
 * @param <T> the type of the values of the keys
 */
public class Keys<T> implements Set<Key<T>> {

    private final Set<Key<T>> keys;

    /**
     * Creates an empty set.
     */
    private Keys() {
        this(0);
    }

    /**
     * Creates an empty set with an initial capacity.
     *
     * @param initialCapacity the initial capacity of the backing set
     */
    private Keys(int initialCapacity) {
        this.keys = new HashSet<>(initialCapacity);
    }

    /**
     * Creates a set that contains a copy of the keys.
     *
     * @param keys the keys to copy
     */
    private Keys(Collection<Key<T>> keys) {
        this.keys = new HashSet<>(keys);
    }

    /**
     * Creates an empty, mutable set.
     *
     * @param <T> the type of the values of the keys
     * @return the empty set
     */
    public static <T> Keys<T> empty() {
        return new Keys<>();
    }

    /**
     * Creates a mutable set that contains the keys.
     *
     * @param keys the keys to add
     * @param <T> the type of the values of the keys
     * @return the set of keys
     */
    @SafeVarargs
    public static <T> Keys<T> of(Key<T>... keys) {
        return new Keys<>(Arrays.asList(keys));
    }

    /**
     * @return the number of keys in the set
     */
    @Override
    public int size() {
        return this.keys.size();
    }

    /**
     * @return {@code true} if the set has no keys
     */
    @Override
    public boolean isEmpty() {
        return this.keys.isEmpty();
    }

    /**
     * Checks if the set contains the object.
     *
     * @param o the object to find
     * @return {@code true} if the set contains the object
     */
    @Override
    public boolean contains(Object o) {
        return this.keys.contains(o);
    }

    /**
     * @return an iterator over the keys of the backing set
     */
    @Override
    public @NonNull Iterator<Key<T>> iterator() {
        return this.keys.iterator();
    }

    /**
     * @return a new array that contains the keys
     */
    @Override
    public @NonNull Object[] toArray() {
        return this.keys.toArray();
    }

    /**
     * Puts the keys in the array, or in a new array of the same type if they do not fit.
     *
     * @param ts the array to fill
     * @param <T> the component type of the array
     * @return the array that contains the keys
     */
    @Override
    public @NonNull <T> T[] toArray(@NonNull T[] ts) {
        return this.keys.toArray(ts);
    }

    /**
     * Adds the key to the set.
     *
     * @param key the key to add
     * @return {@code true} if the set did not contain the key
     */
    @Override
    public boolean add(Key<T> key) {
        return this.keys.add(key);
    }

    /**
     * Removes the object from the set.
     *
     * @param o the object to remove
     * @return {@code true} if the set contained the object
     */
    @Override
    public boolean remove(Object o) {
        return this.keys.remove(o);
    }

    /**
     * Checks if the set contains all objects of the collection.
     *
     * @param collection the objects to find
     * @return {@code true} if the set contains all of them
     */
    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        return this.keys.containsAll(collection);
    }

    /**
     * Adds all keys of the collection to the set.
     *
     * @param collection the keys to add
     * @return {@code true} if the set changed
     */
    @Override
    public boolean addAll(@NonNull Collection<? extends Key<T>> collection) {
        return this.keys.addAll(collection);
    }

    /**
     * Removes all keys that are not in the collection.
     *
     * @param collection the objects to keep
     * @return {@code true} if the set changed
     */
    @Override
    public boolean retainAll(@NonNull Collection<?> collection) {
        return this.keys.retainAll(collection);
    }

    /**
     * Removes all objects of the collection from the set.
     *
     * @param collection the objects to remove
     * @return {@code true} if the set changed
     */
    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        return this.keys.removeAll(collection);
    }

    /**
     * Removes all keys from the set.
     */
    @Override
    public void clear() {
        this.keys.clear();
    }

    /**
     * Checks if the object is this set, or a set that is equal to the backing set.
     *
     * @param o the object to compare with
     * @return {@code true} if the sets are equal
     */
    @Override
    public boolean equals(Object o) {
        return o == this || this.keys.equals(o);
    }

    /**
     * @return the hash code of the backing set
     */
    @Override
    public int hashCode() {
        return this.keys.hashCode();
    }

    /**
     * @return the string form of the backing set
     */
    @Override
    public String toString() {
        return this.keys.toString();
    }
}
