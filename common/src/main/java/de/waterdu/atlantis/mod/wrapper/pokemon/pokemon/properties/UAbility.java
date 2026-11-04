package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;

/**
 * Wraps an ability from the Pokémon mod.
 *
 * @param <T> the type of the wrapped ability
 */
public abstract class UAbility<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for an ability.
     *
     * @param type the key of the mod object type
     * @param ability the ability to wrap
     */
    protected UAbility(ModObjectKey type, T ability) {
        super(new ModObject(type, ability));
    }

    /**
     * @return the name of the ability
     */
    public abstract String name();

    /**
     * @return the name of the ability as a text component
     */
    public abstract Component displayName();
}
