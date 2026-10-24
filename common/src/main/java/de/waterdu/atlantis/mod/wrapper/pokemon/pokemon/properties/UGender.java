package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

public abstract class UGender<T> extends ModWrapper<T> {

    public static final String MALE = "male";
    public static final String FEMALE = "female";
    public static final String GENDERLESS = "genderless";

    protected UGender(ModObjectKey type, T gender) {
        super(new ModObject(type, gender));
    }

    public abstract String name();

    public boolean isMale() {
        return this.name().equals(MALE);
    }

    public boolean isFemale() {
        return this.name().equals(FEMALE);
    }

    public boolean isGenderless() {
        return this.name().equals(GENDERLESS);
    }

    public boolean is(UGender<?> gender) {
        return this.name().equals(gender.name());
    }
}
