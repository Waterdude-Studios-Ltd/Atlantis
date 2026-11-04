package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Wraps a Poké Ball type from the Pokémon mod.
 *
 * @param <T> the type of the wrapped Poké Ball
 */
public abstract class UPokeBall<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for a Poké Ball.
     *
     * @param type the key of the mod object type
     * @param ball the Poké Ball to wrap
     */
    protected UPokeBall(ModObjectKey type, T ball) {
        super(new ModObject(type, ball));
    }

    /**
     * @return the name of the Poké Ball
     */
    public abstract String name();

    /**
     * @return the name of the Poké Ball as a text component
     */
    public abstract Component displayName();

    /**
     * @return an item stack of the Poké Ball
     */
    public abstract ItemStack item();
}
