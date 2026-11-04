package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Wraps the party of a player. The party has {@link #SIZE} slots.
 *
 * @param <T> the type of the party object of the mod
 * @param <P> the type of the Pokémon wrapper
 */
public abstract class UParty<T, P extends UPokemon<?>> extends ModWrapper<T> implements UStorage<P> {

    public static final int SIZE = 6;

    /**
     * Creates a wrapper for a party that has loaded.
     *
     * @param type the key of the mod object type
     * @param party the party object of the mod
     */
    protected UParty(ModObjectKey type, T party) {
        super(new ModObject(type, party));
    }

    /**
     * Creates a wrapper for a party that can still be loading.
     *
     * @param type the key of the mod object type
     * @param party the future that completes with the party object of the mod
     */
    protected UParty(ModObjectKey type, CompletableFuture<? extends T> party) {
        super(new ModFuture(type, party));
    }

    /**
     * Checks if the wrapped party object is available.
     *
     * @return {@code true} if {@link #dereference()} does not give {@code null}
     */
    @Override
    public boolean isLoaded() {
        return this.dereference() != null;
    }

    /**
     * @return {@link #SIZE}
     */
    @Override
    public int capacity() {
        return SIZE;
    }

    /**
     * Gets the Pokémon in a slot of the party.
     *
     * @param slot the slot, from 0 to 5
     * @return the Pokémon, or {@code null} if the slot is empty
     */
    public abstract @Nullable P get(int slot);

    /**
     * @param slot the slot, from 0 to 5
     * @param pokemon the Pokémon to put in the slot, or {@code null} to empty the slot
     * @return the Pokémon that was in the slot before, or {@code null}
     */
    public abstract @Nullable P set(int slot, @Nullable UPokemon<?> pokemon);

    /**
     * Swaps the contents of two slots.
     *
     * @param slot1 the first slot, from 0 to 5
     * @param slot2 the second slot, from 0 to 5
     */
    public abstract void swap(int slot1, int slot2);

    /**
     * Heals every Pokémon in the party.
     */
    public abstract void heal();

    /**
     * Recalls every Pokémon from the party that is currently sent out.
     */
    public abstract void retrieveAll();

    /**
     * Counts the Pokémon in the party that can battle.
     *
     * @return the number of Pokémon that are not eggs and have not fainted
     */
    public int countAble() {
        var count = 0;
        for (var pokemon : this.all()) {
            if (!pokemon.isEgg() && !pokemon.isFainted()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the first Pokémon in the party that can battle.
     *
     * @return the first Pokémon that is not an egg and has not fainted, or {@code null} if there is none
     */
    public @Nullable P firstAble() {
        for (var pokemon : this.all()) {
            if (!pokemon.isEgg() && !pokemon.isFainted()) {
                return pokemon;
            }
        }
        return null;
    }
}
