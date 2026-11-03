package de.waterdu.atlantis.util.java.collection;

import java.util.ArrayList;
import java.util.List;

/**
 * Marks a list of numbers that {@code NumberListTypeAdapter} writes as one compact string, such as {@code "1-3,5"},
 * instead of a JSON array.
 *
 * @param <E> the type of the elements
 */
public class CompactJsonArrayList<E> extends ArrayList<E> {

    /**
     * Creates an empty list.
     */
    public CompactJsonArrayList() {
        super();
    }

    /**
     * Creates an empty list with an initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     */
    public CompactJsonArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Creates a list with the elements of another list.
     *
     * @param list the elements to copy
     */
    public CompactJsonArrayList(List<E> list) {
        super(list);
    }
}
