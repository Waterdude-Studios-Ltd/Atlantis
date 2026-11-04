package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;

/**
 * Wraps an elemental type from the Pokémon mod.
 *
 * @param <T> the type of the wrapped elemental type
 */
public abstract class UElementalType<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for an elemental type.
     *
     * @param type the key of the mod object type
     * @param elementalType the elemental type to wrap
     */
    protected UElementalType(ModObjectKey type, T elementalType) {
        super(new ModObject(type, elementalType));
    }

    /**
     * @return the name of the elemental type
     */
    public abstract String name();

    /**
     * @return the name of the elemental type as a text component
     */
    public abstract Component displayName();

    /**
     * Checks if this elemental type has the same name as another one. The check ignores case.
     *
     * @param type the elemental type to compare with
     * @return {@code true} if the names are equal
     */
    public boolean is(UElementalType<?> type) {
        return this.name().equalsIgnoreCase(type.name());
    }
}
