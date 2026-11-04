package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;

/**
 * Wraps a gender from the Pokémon mod.
 *
 * @param <T> the type of the wrapped gender
 */
public abstract class UGender<T> extends ModWrapper<T> {

    public static final String MALE = "male";
    public static final String FEMALE = "female";
    public static final String GENDERLESS = "genderless";

    /**
     * Creates a wrapper for a gender.
     *
     * @param type the key of the mod object type
     * @param gender the gender to wrap
     */
    protected UGender(ModObjectKey type, T gender) {
        super(new ModObject(type, gender));
    }

    /**
     * @return the name of the gender, such as {@link #MALE} or {@link #GENDERLESS}
     */
    public abstract String name();

    /**
     * Checks if the gender is male.
     *
     * @return {@code true} if the name is {@link #MALE}
     */
    public boolean isMale() {
        return this.name().equals(MALE);
    }

    /**
     * Checks if the gender is female.
     *
     * @return {@code true} if the name is {@link #FEMALE}
     */
    public boolean isFemale() {
        return this.name().equals(FEMALE);
    }

    /**
     * Checks if the gender is genderless.
     *
     * @return {@code true} if the name is {@link #GENDERLESS}
     */
    public boolean isGenderless() {
        return this.name().equals(GENDERLESS);
    }

    /**
     * Checks if this gender has the same name as another gender.
     *
     * @param gender the gender to compare with
     * @return {@code true} if the names are equal
     */
    public boolean is(UGender<?> gender) {
        return this.name().equals(gender.name());
    }
}
