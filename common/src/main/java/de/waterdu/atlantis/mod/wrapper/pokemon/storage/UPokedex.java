package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;

import java.util.concurrent.CompletableFuture;

/**
 * Wraps the Pokédex of a player.
 *
 * @param <T> the type of the Pokédex object of the mod
 */
public abstract class UPokedex<T> extends ModWrapper<T> {

    /**
     * Creates a wrapper for a Pokédex that has loaded.
     *
     * @param type the key of the mod object type
     * @param pokedex the Pokédex object of the mod
     */
    protected UPokedex(ModObjectKey type, T pokedex) {
        super(new ModObject(type, pokedex));
    }

    /**
     * Creates a wrapper for a Pokédex that can still be loading.
     *
     * @param type the key of the mod object type
     * @param pokedex the future that completes with the Pokédex object of the mod
     */
    protected UPokedex(ModObjectKey type, CompletableFuture<? extends T> pokedex) {
        super(new ModFuture(type, pokedex));
    }

    /**
     * Checks if the wrapped Pokédex object is available.
     *
     * @return {@code true} if {@link #dereference()} does not give {@code null}
     */
    public boolean isLoaded() {
        return this.dereference() != null;
    }

    /**
     * Checks if the player has seen the species.
     *
     * @param species the species to check
     * @return {@code true} if the species is marked as seen
     */
    public abstract boolean hasSeen(USpecies<?> species);

    /**
     * Checks if the player has caught the species.
     *
     * @param species the species to check
     * @return {@code true} if the species is marked as caught
     */
    public abstract boolean hasCaught(USpecies<?> species);

    /**
     * @return the number of species that the player has seen
     */
    public abstract int seenCount();

    /**
     * @return the number of species that the player has caught
     */
    public abstract int caughtCount();

    /**
     * Marks the Pokémon as seen in the Pokédex.
     *
     * @param pokemon the Pokémon to mark
     */
    public abstract void setSeen(UPokemon<?> pokemon);

    /**
     * Marks the Pokémon as caught in the Pokédex.
     *
     * @param pokemon the Pokémon to mark
     */
    public abstract void setCaught(UPokemon<?> pokemon);
}
