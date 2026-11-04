package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.storage;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UParty;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wraps the Cobblemon party of a player.
 */
public class CobblemonParty extends UParty<PartyStore, CobblemonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "party");

    /**
     * Creates a wrapper for a Cobblemon party.
     *
     * @param party the Cobblemon party store to wrap
     */
    public CobblemonParty(PartyStore party) {
        super(TYPE, party);
    }

    /**
     * @return the UUID of the party store
     */
    @Override
    public UUID owner() {
        return this.dereference().getUuid();
    }

    /**
     * Gets every Pokémon in this party.
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
     * Checks if this party has an empty slot.
     *
     * @return {@code true} if the party has at least one empty slot
     */
    @Override
    public boolean hasSpace() {
        return this.dereference().getFirstAvailablePosition() != null;
    }

    /**
     * Adds the Pokémon to the first empty slot of this party.
     *
     * @param pokemon the Pokémon to add
     * @return {@code true} if Cobblemon added the Pokémon
     */
    @Override
    public boolean add(UPokemon<?> pokemon) {
        return this.dereference().add(CobblemonPokemon.unwrap(pokemon));
    }

    /**
     * Removes the Pokémon from this party.
     *
     * @param pokemon the Pokémon to remove
     * @return {@code true} if Cobblemon removed the Pokémon
     */
    @Override
    public boolean remove(UPokemon<?> pokemon) {
        return this.dereference().remove(CobblemonPokemon.unwrap(pokemon));
    }

    /**
     * Finds the Pokémon with the given UUID in this party.
     *
     * @param uuid the UUID of the Pokémon
     * @return the Pokémon, or {@code null} if the party does not have it
     */
    @Override
    public @Nullable CobblemonPokemon find(UUID uuid) {
        return CobblemonPokemon.of(this.dereference().get(uuid));
    }

    /**
     * Gets the Pokémon in the given slot.
     *
     * @param slot the slot, from 0 to 5
     * @return the Pokémon, or {@code null} if the slot is empty
     */
    @Override
    public @Nullable CobblemonPokemon get(int slot) {
        return CobblemonPokemon.of(this.dereference().get(slot));
    }

    /**
     * Puts a Pokémon in the given slot, or empties the slot.
     *
     * @param slot the slot, from 0 to 5
     * @param pokemon the Pokémon to put in the slot, or {@code null} to empty the slot
     * @return the Pokémon that was in the slot before, or {@code null}
     */
    @Override
    public @Nullable CobblemonPokemon set(int slot, @Nullable UPokemon<?> pokemon) {
        var party = this.dereference();
        var old = this.get(slot);
        if (pokemon == null) {
            party.remove(new PartyPosition(slot));
        } else {
            party.set(slot, CobblemonPokemon.unwrap(pokemon));
        }
        return old;
    }

    /**
     * Swaps the Pokémon in two slots.
     *
     * @param slot1 the first slot
     * @param slot2 the second slot
     */
    @Override
    public void swap(int slot1, int slot2) {
        this.dereference().swap(slot1, slot2);
    }

    /**
     * Heals every Pokémon in this party.
     */
    @Override
    public void heal() {
        this.dereference().heal();
    }

    /**
     * Recalls every Pokémon in this party that is out as an entity. Cobblemon plays the recall animation.
     */
    @Override
    public void retrieveAll() {
        for (var pokemon : this.dereference()) {
            if (pokemon.getEntity() != null) {
                pokemon.tryRecallWithAnimation();
            }
        }
    }
}
