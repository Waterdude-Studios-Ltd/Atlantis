package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.parsing;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.item.PokemonItem;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import net.minecraft.world.item.ItemStack;

/**
 * Wraps Cobblemon Pokémon properties that were parsed from a spec string.
 */
public class CobblemonPokemonSpec extends UPokemonSpec<PokemonProperties, CobblemonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "spec");

    /**
     * Creates a wrapper for Cobblemon Pokémon properties.
     *
     * @param spec the properties to wrap
     */
    public CobblemonPokemonSpec(PokemonProperties spec) {
        super(TYPE, spec);
    }

    /**
     * Creates a new Pokémon from the properties.
     *
     * @return the new Pokémon
     */
    @Override
    public CobblemonPokemon create() {
        return new CobblemonPokemon(this.dereference().create());
    }

    /**
     * @return a Cobblemon Pokémon item made from the properties
     */
    @Override
    public ItemStack item() {
        return PokemonItem.from(this.dereference());
    }

    /**
     * Checks if the Pokémon matches the properties.
     *
     * @param pokemon the Cobblemon Pokémon to check
     * @return {@code true} if the Pokémon matches
     * @throws IllegalArgumentException if the Pokémon is not a Cobblemon Pokémon
     */
    @Override
    public boolean matches(UPokemon<?> pokemon) {
        return this.dereference().matches(CobblemonPokemon.unwrap(pokemon));
    }

    /**
     * Counts the aspects of the properties.
     *
     * @return the number of aspects
     */
    @Override
    public int countRequirements() {
        return this.dereference().getAspects().size();
    }

    /**
     * @return the string that the properties were parsed from
     */
    @Override
    public String originalString() {
        return this.dereference().getOriginalString();
    }
}
