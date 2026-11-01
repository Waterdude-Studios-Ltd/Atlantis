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

    boolean isLoaded();

    UUID owner();

    int capacity();

    /**
     * Every Pokémon in this storage, without the empty slots.
     */
    List<P> all();

    boolean hasSpace();

    /**
     * Puts the Pokémon in the first free slot. If the party is full, the Pokémon goes to the PC instead.
     */
    boolean add(UPokemon<?> pokemon);

    boolean remove(UPokemon<?> pokemon);

    @Nullable P find(UUID uuid);

    default int count() {
        return this.all().size();
    }

    default boolean isEmpty() {
        return this.count() == 0;
    }

    default boolean contains(UUID uuid) {
        return this.find(uuid) != null;
    }

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
