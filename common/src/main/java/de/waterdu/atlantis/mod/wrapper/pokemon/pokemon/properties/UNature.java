package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a nature from the Pokémon mod.
 *
 * @param <T> the type of the wrapped nature
 */
public abstract class UNature<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for a nature.
     *
     * @param type the key of the mod object type
     * @param nature the nature to wrap
     */
    protected UNature(ModObjectKey type, T nature) {
        super(new ModObject(type, nature));
    }

    /**
     * @return the name of the nature
     */
    public abstract String name();

    /**
     * @return the name of the nature as a text component
     */
    public abstract Component displayName();

    /**
     * Gets the stat that the nature increases.
     *
     * @return the stat, or {@code null} if the nature is neutral
     */
    public abstract @Nullable UStat<?> increased();

    /**
     * Gets the stat that the nature decreases.
     *
     * @return the stat, or {@code null} if the nature is neutral
     */
    public abstract @Nullable UStat<?> decreased();

    /**
     * Checks if the nature changes no stats.
     *
     * @return {@code true} if {@link #increased()} is {@code null}
     */
    public boolean isNeutral() {
        return this.increased() == null;
    }

    /**
     * Checks if this nature has the same name as another nature.
     *
     * @param nature the nature to compare with
     * @return {@code true} if the names are equal
     */
    public boolean is(UNature<?> nature) {
        return this.name().equals(nature.name());
    }
}
