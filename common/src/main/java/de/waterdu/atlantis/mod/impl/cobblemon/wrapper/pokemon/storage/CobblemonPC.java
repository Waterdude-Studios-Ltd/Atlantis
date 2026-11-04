package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.storage;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.pc.ConstantsKt;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wraps the Cobblemon PC of a player.
 */
public class CobblemonPC extends UPC<PCStore, CobblemonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pc");

    /**
     * Creates a wrapper for a Cobblemon PC.
     *
     * @param pc the Cobblemon PC store to wrap
     */
    public CobblemonPC(PCStore pc) {
        super(TYPE, pc);
    }

    /**
     * @return the UUID of the PC store
     */
    @Override
    public UUID owner() {
        return this.dereference().getUuid();
    }

    /**
     * Gets every Pokémon in this PC.
     *
     * @return a new list of the Pokémon
     */
    @Override
    public List<CobblemonPokemon> all() {
        var all = new ArrayList<CobblemonPokemon>();
        for (var pokemon : this.dereference()) {
            all.add(new CobblemonPokemon(pokemon));
        }
        return all;
    }

    /**
     * Checks if this PC has an empty slot.
     *
     * @return {@code true} if the PC has at least one empty slot
     */
    @Override
    public boolean hasSpace() {
        return this.dereference().getFirstAvailablePosition() != null;
    }

    /**
     * Adds the Pokémon to the first empty slot of this PC.
     *
     * @param pokemon the Pokémon to add
     * @return {@code true} if Cobblemon added the Pokémon
     */
    @Override
    public boolean add(UPokemon<?> pokemon) {
        return this.dereference().add(CobblemonPokemon.unwrap(pokemon));
    }

    /**
     * Removes the Pokémon from this PC.
     *
     * @param pokemon the Pokémon to remove
     * @return {@code true} if Cobblemon removed the Pokémon
     */
    @Override
    public boolean remove(UPokemon<?> pokemon) {
        return this.dereference().remove(CobblemonPokemon.unwrap(pokemon));
    }

    /**
     * Finds the Pokémon with the given UUID in this PC.
     *
     * @param uuid the UUID of the Pokémon
     * @return the Pokémon, or {@code null} if the PC does not have it
     */
    @Override
    public @Nullable CobblemonPokemon find(UUID uuid) {
        return CobblemonPokemon.of(this.dereference().get(uuid));
    }

    /**
     * Gets the Pokémon at the given position.
     *
     * @param box the box index
     * @param slot the slot index in the box
     * @return the Pokémon, or {@code null} if the slot is empty
     */
    @Override
    public @Nullable CobblemonPokemon get(int box, int slot) {
        return CobblemonPokemon.of(this.dereference().get(new PCPosition(box, slot)));
    }

    /**
     * Puts a Pokémon at the given position, or empties the position.
     *
     * @param pokemon the Pokémon to put in the slot, or {@code null} to empty the slot
     * @param box the box index
     * @param slot the slot index in the box
     * @return the Pokémon that was in the slot before, or {@code null}
     */
    @Override
    public @Nullable CobblemonPokemon set(@Nullable UPokemon<?> pokemon, int box, int slot) {
        var pc = this.dereference();
        var position = new PCPosition(box, slot);
        var old = CobblemonPokemon.of(pc.get(position));
        if (pokemon == null) {
            pc.remove(position);
        } else {
            pc.set(position, CobblemonPokemon.unwrap(pokemon));
        }
        return old;
    }

    /**
     * @return the number of boxes in the PC
     */
    @Override
    public int boxCount() {
        return this.dereference().getBoxes().size();
    }

    /**
     * @return the number of slots in each box, from {@link ConstantsKt#POKEMON_PER_BOX}
     */
    @Override
    public int boxSize() {
        return ConstantsKt.POKEMON_PER_BOX;
    }
}
