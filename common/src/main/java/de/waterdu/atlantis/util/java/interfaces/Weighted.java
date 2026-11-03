package de.waterdu.atlantis.util.java.interfaces;

/**
 * Has a weight, for example for a random choice in a {@link de.waterdu.atlantis.util.java.collection.WeightedSet}.
 */
public interface Weighted {

    /**
     * @return the weight of this object
     */
    double weight();
}
