package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;

import java.util.List;

/**
 * Wraps a form of a species from the Pokémon mod.
 *
 * @param <T> the type of the wrapped form
 */
public abstract class UForm<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for a form.
     *
     * @param type the key of the mod object type
     * @param form the form to wrap
     */
    protected UForm(ModObjectKey type, T form) {
        super(new ModObject(type, form));
    }

    /**
     * @return the name of the form
     */
    public abstract String name();

    /**
     * @return the species that the form belongs to
     */
    public abstract USpecies<?> species();

    /**
     * @return the elemental types of the form
     */
    public abstract List<? extends UElementalType<?>> types();

    /**
     * Gets the base value of a stat for the form.
     *
     * @param stat the stat to get
     * @return the base value of the stat
     */
    public abstract int baseStat(UStat<?> stat);

    /**
     * @return the sum of the base stats of the form
     */
    public abstract int baseStatTotal();

    /**
     * @return the catch rate of the form
     */
    public abstract int catchRate();

    /**
     * Checks if the form has an elemental type.
     *
     * @param type the elemental type to find
     * @return {@code true} if one of the {@link #types()} is the given type
     */
    public boolean hasType(UElementalType<?> type) {
        for (var elementalType : this.types()) {
            if (elementalType.is(type)) {
                return true;
            }
        }
        return false;
    }
}
