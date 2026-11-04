package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.storage;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPokedex;

/**
 * Wraps the Cobblemon Pokédex of a player.
 */
public class CobblemonPokedex extends UPokedex<PokedexManager> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pokedex");

    /**
     * Creates a wrapper for a Cobblemon Pokédex.
     *
     * @param pokedex the Cobblemon Pokédex manager to wrap
     */
    public CobblemonPokedex(PokedexManager pokedex) {
        super(TYPE, pokedex);
    }

    /**
     * Checks if the player has seen the given species.
     *
     * @param species the species to check
     * @return {@code true} if the species is seen or caught
     */
    @Override
    public boolean hasSeen(USpecies<?> species) {
        return this.knowledge((Species) species.dereference()) != PokedexEntryProgress.UNREGISTERED;
    }

    /**
     * Checks if the player has caught the given species.
     *
     * @param species the species to check
     * @return {@code true} if the species is caught
     */
    @Override
    public boolean hasCaught(USpecies<?> species) {
        return this.knowledge((Species) species.dereference()) == PokedexEntryProgress.OWNED;
    }

    /**
     * Counts the implemented species that the player has seen or caught.
     *
     * @return the number of seen species
     */
    @Override
    public int seenCount() {
        return this.count(PokedexEntryProgress.SEEN);
    }

    /**
     * Counts the implemented species that the player has caught.
     *
     * @return the number of caught species
     */
    @Override
    public int caughtCount() {
        return this.count(PokedexEntryProgress.OWNED);
    }

    /**
     * Records an encounter with the Pokémon in this Pokédex.
     *
     * @param pokemon the Pokémon that the player saw
     */
    @Override
    public void setSeen(UPokemon<?> pokemon) {
        this.dereference().encounter(CobblemonPokemon.unwrap(pokemon));
    }

    /**
     * Records that the player obtained the Pokémon in this Pokédex.
     *
     * @param pokemon the Pokémon that the player caught
     */
    @Override
    public void setCaught(UPokemon<?> pokemon) {
        this.dereference().obtain(CobblemonPokemon.unwrap(pokemon));
    }

    /**
     * Gets the Pokédex progress of the player for the given species.
     *
     * @param species the Cobblemon species
     * @return the progress for the species
     */
    private PokedexEntryProgress knowledge(Species species) {
        return this.dereference().getKnowledgeForSpecies(species.getResourceIdentifier());
    }

    /**
     * Counts the implemented species with at least the given progress.
     *
     * @param atLeast the lowest progress to count
     * @return the number of species
     */
    private int count(PokedexEntryProgress atLeast) {
        var count = 0;
        for (var species : PokemonSpecies.getImplemented()) {
            if (this.knowledge(species).ordinal() >= atLeast.ordinal()) {
                count++;
            }
        }
        return count;
    }
}
