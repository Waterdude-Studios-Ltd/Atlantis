package de.waterdu.atlantis.mod.wrapper.pokemon.storage;

import de.waterdu.atlantis.mod.object.ModFuture;
import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Wraps the PC storage of a player. The PC has {@link #boxCount()} boxes with {@link #boxSize()} slots each.
 *
 * @param <T> the type of the PC object of the mod
 * @param <P> the type of the Pokémon wrapper
 */
public abstract class UPC<T, P extends UPokemon<?>> extends ModWrapper<T> implements UStorage<P> {

    /**
     * Creates a wrapper for a PC that has loaded.
     *
     * @param type the key of the mod object type
     * @param pc the PC object of the mod
     */
    protected UPC(ModObjectKey type, T pc) {
        super(new ModObject(type, pc));
    }

    /**
     * Creates a wrapper for a PC that can still be loading.
     *
     * @param type the key of the mod object type
     * @param pc the future that completes with the PC object of the mod
     */
    protected UPC(ModObjectKey type, CompletableFuture<T> pc) {
        super(new ModFuture(type, pc));
    }

    /**
     * Checks if the wrapped PC object is available.
     *
     * @return {@code true} if {@link #dereference()} does not give {@code null}
     */
    @Override
    public boolean isLoaded() {
        return this.dereference() != null;
    }

    /**
     * Gets the number of slots in all boxes.
     *
     * @return the box count multiplied by the box size
     */
    @Override
    public int capacity() {
        return this.boxCount() * this.boxSize();
    }

    /**
     * Gets the Pokémon in a slot of a box.
     *
     * @param box the index of the box
     * @param slot the slot in the box
     * @return the Pokémon, or {@code null} if the slot is empty
     */
    public abstract @Nullable P get(int box, int slot);

    /**
     * @param pokemon the Pokémon to put in the slot, or {@code null} to empty the slot
     * @return the Pokémon that was in the slot before, or {@code null}
     */
    public abstract @Nullable P set(@Nullable UPokemon<?> pokemon, int box, int slot);

    /**
     * @return the number of boxes in the PC
     */
    public abstract int boxCount();

    /**
     * @return the number of slots in each box
     */
    public abstract int boxSize();

    /**
     * Gets the Pokémon in a box.
     *
     * @param box the index of the box
     * @return the Pokémon in the box, without the empty slots
     */
    public List<P> box(int box) {
        var pokemon = new ArrayList<P>();
        for (var slot = 0; slot < this.boxSize(); slot++) {
            var found = this.get(box, slot);
            if (found != null) {
                pokemon.add(found);
            }
        }
        return pokemon;
    }
}
