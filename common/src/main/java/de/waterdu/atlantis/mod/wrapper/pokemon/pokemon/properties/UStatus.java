package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

/**
 * Wraps a status condition of a Pokémon mod, such as burn or sleep.
 *
 * @param <T> the type of the status object of the mod
 */
public abstract class UStatus<T> extends ModWrapper<T> {

    public static final String BURN = "burn";
    public static final String FREEZE = "freeze";
    public static final String PARALYSIS = "paralysis";
    public static final String POISON = "poison";
    public static final String BAD_POISON = "bad_poison";
    public static final String SLEEP = "sleep";

    /**
     * Creates a wrapper for the status.
     *
     * @param type the key of the mod object type
     * @param status the status object of the mod
     */
    protected UStatus(ModObjectKey type, T status) {
        super(new ModObject(type, status));
    }

    /**
     * @return the name of the status, such as {@link #BURN}
     */
    public abstract String name();

    /**
     * Checks if this status has the same name as the other status.
     *
     * @param status the status to compare with
     * @return {@code true} if the names are equal
     */
    public boolean is(UStatus<?> status) {
        return this.name().equals(status.name());
    }
}
