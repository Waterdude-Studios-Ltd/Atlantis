package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Pokémon storage that belongs to a player. Storage can load asynchronously. If the storage had not loaded when the
 * wrapper was looked up, the wrapper stays unloaded. Then {@link #isLoaded()} is false, reads return empty results, and
 * writes do nothing. In that case, wait on {@code future()} and look up the storage again.
 */
public interface UStorage<P extends UPokemon<?>> {

    /**
     * Checks if the storage had loaded when the wrapper was looked up.
     *
     * @return {@code true} if reads and writes reach the storage
     */
    boolean isLoaded();

    /**
     * @return the UUID of the player that owns this storage
     */
    UUID owner();

    /**
     * @return the total number of slots in this storage
     */
    int capacity();

    /**
     * Every Pokémon in this storage, without the empty slots.
     */
    List<P> all();

    /**
     * Checks if this storage has a free slot.
     *
     * @return {@code true} if a slot is free
     */
    boolean hasSpace();

    /**
     * Puts the Pokémon in the first free slot. If the party is full, the Pokémon goes to the PC instead.
     */
    boolean add(UPokemon<?> pokemon);

    /**
     * Removes the Pokémon from this storage.
     *
     * @param pokemon the Pokémon to remove
     * @return {@code true} if the Pokémon was removed
     */
    boolean remove(UPokemon<?> pokemon);

    /**
     * Finds the Pokémon with the UUID in this storage.
     *
     * @param uuid the UUID of the Pokémon
     * @return the Pokémon, or {@code null} if this storage does not have it
     */
    @Nullable P find(UUID uuid);

    /**
     * @return the number of Pokémon in this storage
     */
    default int count() {
        return this.all().size();
    }

    /**
     * Checks if this storage has no Pokémon.
     *
     * @return {@code true} if {@link #count()} is 0
     */
    default boolean isEmpty() {
        return this.count() == 0;
    }

    /**
     * Checks if this storage has the Pokémon with the UUID.
     *
     * @param uuid the UUID of the Pokémon
     * @return {@code true} if {@link #find(UUID)} finds the Pokémon
     */
    default boolean contains(UUID uuid) {
        return this.find(uuid) != null;
    }

    /**
     * Finds every Pokémon in this storage that matches the predicate.
     *
     * @param predicate the test that each Pokémon must pass
     * @return the matching Pokémon, in the order of {@link #all()}
     */
    default List<P> findAll(Predicate<? super P> predicate) {
        var found = new ArrayList<P>();
        for (var pokemon : this.all()) {
            if (predicate.test(pokemon)) {
                found.add(pokemon);
            }
        }
        return found;
    }
}
