package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.storage.PCBox;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Wraps the Pixelmon {@link PCStorage} of a player.
 */
public class PixelmonPC extends UPC<PCStorage, PixelmonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pc");

    private final UUID owner;

    /**
     * Creates a wrapper for the Pixelmon PC.
     *
     * @param owner the UUID of the player who owns the PC
     * @param pc the future that gives the Pixelmon PC storage
     */
    public PixelmonPC(UUID owner, CompletableFuture<PCStorage> pc) {
        super(TYPE, pc);
        this.owner = owner;
    }

    /**
     * @return the UUID of the player who owns the PC
     */
    @Override
    public UUID owner() {
        return this.owner;
    }

    /**
     * Gets wrappers for all Pokémon in the PC. Empty slots are skipped.
     *
     * @return a new list of the Pokémon, which is empty if the PC is not loaded
     */
    @Override
    public List<PixelmonPokemon> all() {
        var all = new ArrayList<PixelmonPokemon>();
        if (this.isLoaded()) {
            for (var pokemon : this.dereference().getAll()) {
                if (pokemon != null) {
                    all.add(new PixelmonPokemon(pokemon));
                }
            }
        }
        return all;
    }

    /**
     * Checks if the PC has an empty slot.
     *
     * @return {@code false} if the PC is full or not loaded
     */
    @Override
    public boolean hasSpace() {
        return this.isLoaded() && this.dereference().hasSpace();
    }

    /**
     * Adds the Pokémon to the PC.
     *
     * @param pokemon the Pokémon to add
     * @return {@code true} if Pixelmon added the Pokémon, or {@code false} if the PC is not loaded
     */
    @Override
    public boolean add(UPokemon<?> pokemon) {
        return this.isLoaded() && this.dereference().add(PixelmonPokemon.unwrap(pokemon));
    }

    /**
     * Removes the Pokémon from its slot in the PC.
     *
     * @param pokemon the Pokémon to remove
     * @return {@code false} if the PC is not loaded or does not hold the Pokémon
     */
    @Override
    public boolean remove(UPokemon<?> pokemon) {
        if (!this.isLoaded()) {
            return false;
        }
        var pc = this.dereference();
        var position = pc.getPosition(PixelmonPokemon.unwrap(pokemon));
        if (position == null) {
            return false;
        }
        pc.set(position, null);
        return true;
    }

    /**
     * Finds the Pokémon with the UUID in the PC.
     *
     * @param uuid the UUID of the Pokémon
     * @return the Pokémon, or {@code null} if the PC is not loaded or has no such Pokémon
     */
    @Override
    public @Nullable PixelmonPokemon find(UUID uuid) {
        return this.isLoaded() ? PixelmonPokemon.of(this.dereference().find(uuid)) : null;
    }

    /**
     * Gets the Pokémon in the slot of the box.
     *
     * @param box the index of the box
     * @param slot the index of the slot in the box
     * @return the Pokémon, or {@code null} if the PC is not loaded or the slot is empty
     */
    @Override
    public @Nullable PixelmonPokemon get(int box, int slot) {
        return this.isLoaded() ? PixelmonPokemon.of(this.dereference().get(box, slot)) : null;
    }

    /**
     * Puts the Pokémon in the slot of the box. Does nothing if the PC is not loaded.
     *
     * @param pokemon the Pokémon to put in the slot, or {@code null} to empty the slot
     * @param box the index of the box
     * @param slot the index of the slot in the box
     * @return the Pokémon that was in the slot before, or {@code null} if there was none
     */
    @Override
    public @Nullable PixelmonPokemon set(@Nullable UPokemon<?> pokemon, int box, int slot) {
        if (!this.isLoaded()) {
            return null;
        }
        var old = this.get(box, slot);
        this.dereference().set(box, slot, pokemon == null ? null : PixelmonPokemon.unwrap(pokemon));
        return old;
    }

    /**
     * @return the number of boxes in the PC, or {@code 0} if the PC is not loaded
     */
    @Override
    public int boxCount() {
        return this.isLoaded() ? this.dereference().getBoxCount() : 0;
    }

    /**
     * @return {@link PCBox#POKEMON_PER_BOX}
     */
    @Override
    public int boxSize() {
        return PCBox.POKEMON_PER_BOX;
    }
}
