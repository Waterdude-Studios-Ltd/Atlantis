package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Pokémon storage belonging to a player. Storage may load asynchronously. A wrapper whose storage had not loaded when
 * it was looked up stays unloaded: {@link #isLoaded()} is false, reads return empty results and writes do nothing. Wait
 * on {@code future()} and look the storage up again in that case.
 */
public interface UStorage<P extends UPokemon<?>> {

    boolean isLoaded();

    UUID owner();

    int capacity();

    /**
     * Every Pokémon in this storage, skipping empty slots.
     */
    List<P> all();

    boolean hasSpace();

    /**
     * Puts the Pokémon in the first free slot. A party that is full sends the Pokémon to the PC instead.
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
