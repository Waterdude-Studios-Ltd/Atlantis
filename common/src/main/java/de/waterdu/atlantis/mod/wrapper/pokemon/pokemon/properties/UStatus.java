package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class UStatus<T> extends ModWrapper<T> {

    public static final String BURN = "burn";
    public static final String FREEZE = "freeze";
    public static final String PARALYSIS = "paralysis";
    public static final String POISON = "poison";
    public static final String BAD_POISON = "bad_poison";
    public static final String SLEEP = "sleep";

    protected UStatus(ModObjectKey type, T status) {
        super(new ModObject(type, status));
    }

    public abstract String name();

    public boolean is(UStatus<?> status) {
        return this.name().equals(status.name());
    }
}
