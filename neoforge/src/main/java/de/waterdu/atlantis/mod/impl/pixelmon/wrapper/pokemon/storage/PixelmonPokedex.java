package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokedex.PokedexStorage;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonBase;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPokedex;

import java.util.concurrent.CompletableFuture;

/**
 * Wraps the Pixelmon {@link PokedexStorage} of a player.
 */
public class PixelmonPokedex extends UPokedex<PokedexStorage> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pokedex");

    /**
     * Creates a wrapper for the Pixelmon Pokédex.
     *
     * @param pokedex the future that gives the Pixelmon Pokédex storage
     */
    public PixelmonPokedex(CompletableFuture<PokedexStorage> pokedex) {
        super(TYPE, pokedex);
    }

    /**
     * Checks if the Pokédex has the species as seen.
     *
     * @param species the species to check
     * @return {@code false} if the species is not seen or the Pokédex is not loaded
     */
    @Override
    public boolean hasSeen(USpecies<?> species) {
        return this.isLoaded() && this.dereference().hasSeen(new PokemonBase((Species) species.dereference()));
    }

    /**
     * Checks if the Pokédex has the species as caught.
     *
     * @param species the species to check
     * @return {@code false} if the species is not caught or the Pokédex is not loaded
     */
    @Override
    public boolean hasCaught(USpecies<?> species) {
        return this.isLoaded() && this.dereference().hasCaught(new PokemonBase((Species) species.dereference()));
    }

    /**
     * @return the number of seen entries, or {@code 0} if the Pokédex is not loaded
     */
    @Override
    public int seenCount() {
        return this.isLoaded() ? this.dereference().countSeen() : 0;
    }

    /**
     * @return the number of caught entries, or {@code 0} if the Pokédex is not loaded
     */
    @Override
    public int caughtCount() {
        return this.isLoaded() ? this.dereference().countCaught() : 0;
    }

    /**
     * Marks the Pokémon as seen, with {@link Atlantis#ID} as the source. Does nothing if the Pokédex is not loaded.
     *
     * @param pokemon the Pokémon to mark as seen
     */
    @Override
    public void setSeen(UPokemon<?> pokemon) {
        if (this.isLoaded()) {
            this.dereference().setSeen(PixelmonPokemon.unwrap(pokemon), Atlantis.ID);
        }
    }

    /**
     * Marks the Pokémon as caught, with {@link Atlantis#ID} as the source. Does nothing if the Pokédex is not loaded.
     *
     * @param pokemon the Pokémon to mark as caught
     */
    @Override
    public void setCaught(UPokemon<?> pokemon) {
        if (this.isLoaded()) {
            this.dereference().setCaught(PixelmonPokemon.unwrap(pokemon), Atlantis.ID);
        }
    }
}
