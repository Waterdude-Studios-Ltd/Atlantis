package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wraps a species from the Pokémon mod.
 *
 * @param <T> the type of the wrapped species
 */
public abstract class USpecies<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for a species.
     *
     * @param type the key of the mod object type
     * @param species the species to wrap
     */
    protected USpecies(ModObjectKey type, T species) {
        super(new ModObject(type, species));
    }

    /**
     * @return the translated name as plain text. Use {@link #id()} to identify the species.
     */
    public abstract String name();

    /**
     * @return the ID of the species, which does not depend on the language
     */
    public abstract String id();

    /**
     * @return the translated name of the species as a text component
     */
    public abstract Component displayName();

    /**
     * @return the National Pokédex number of the species
     */
    public abstract int dex();

    /**
     * @return the form that the species has by default
     */
    public abstract UForm<?> defaultForm();

    /**
     * @return all forms of the species
     */
    public abstract List<? extends UForm<?>> forms();

    /**
     * Gets a form of the species by its name.
     *
     * @param name the name of the form
     * @return the form, or {@code null} if the species has no form with this name
     */
    public abstract @Nullable UForm<?> form(String name);

    /**
     * Checks if the species is legendary.
     *
     * @return {@code true} if the species is legendary
     */
    public abstract boolean isLegendary();

    /**
     * Checks if the species is mythical.
     *
     * @return {@code true} if the species is mythical
     */
    public abstract boolean isMythical();

    /**
     * Checks if the species is an Ultra Beast.
     *
     * @return {@code true} if the species is an Ultra Beast
     */
    public abstract boolean isUltraBeast();

    /**
     * Creates a new Pokémon of the species.
     *
     * @param level the level of the new Pokémon
     * @return the new Pokémon
     */
    public abstract UPokemon<?> create(int level);

    /**
     * Checks if this species is the same as another species. The Pokédex numbers must be equal, and the IDs must be
     * equal when case is ignored.
     *
     * @param species the species to compare with
     * @return {@code true} if both species are the same
     */
    public boolean is(USpecies<?> species) {
        return this.dex() == species.dex() && this.id().equalsIgnoreCase(species.id());
    }
}
