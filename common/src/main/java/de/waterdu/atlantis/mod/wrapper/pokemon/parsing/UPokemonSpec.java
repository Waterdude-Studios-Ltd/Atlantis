package de.waterdu.atlantis.mod.wrapper.pokemon.parsing;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.world.item.ItemStack;

/**
 * Wraps a Pokémon specification from the Pokémon mod. A spec describes a Pokémon as a string of requirements.
 *
 * @param <T> the type of the wrapped spec
 * @param <P> the type of Pokémon that the spec creates
 */
public abstract class UPokemonSpec<T, P extends UPokemon<?>> extends ModWrapper<T> {

    /**
     * Creates a wrapper for a Pokémon spec.
     *
     * @param type the key of the mod object type
     * @param spec the spec to wrap
     */
    protected UPokemonSpec(ModObjectKey type, T spec) {
        super(new ModObject(type, spec));
    }

    /**
     * Creates a new Pokémon from the spec.
     *
     * @return the new Pokémon
     */
    public abstract P create();

    /**
     * @return a display item that shows a Pokémon made from the spec
     */
    public abstract ItemStack item();

    /**
     * Checks if a Pokémon meets the requirements of the spec.
     *
     * @param pokemon the Pokémon to check
     * @return {@code true} if the Pokémon matches the spec
     */
    public abstract boolean matches(UPokemon<?> pokemon);

    /**
     * @return the number of requirements in the spec
     */
    public abstract int countRequirements();

    /**
     * @return the spec as a string
     */
    public abstract String originalString();
}
