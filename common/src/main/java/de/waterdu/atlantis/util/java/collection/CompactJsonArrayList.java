package de.waterdu.atlantis.util.java.collection;

import java.util.ArrayList;
import java.util.List;

public class CompactJsonArrayList<E> extends ArrayList<E> {

    public CompactJsonArrayList() {
        super();
    }

    public CompactJsonArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    public CompactJsonArrayList(List<E> list) {
        super(list);
    }
}
