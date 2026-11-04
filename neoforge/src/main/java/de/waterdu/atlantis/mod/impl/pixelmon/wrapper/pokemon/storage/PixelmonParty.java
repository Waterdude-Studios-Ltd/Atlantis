package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.PokemonRetrievedEvent;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.UPokemon;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UParty;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Wraps the Pixelmon {@link PartyStorage} of a player.
 */
public class PixelmonParty extends UParty<PartyStorage, PixelmonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "party");

    private final UUID owner;

    /**
     * Creates a wrapper for the Pixelmon party.
     *
     * @param owner the UUID of the player who owns the party
     * @param party the future that gives the Pixelmon party storage
     */
    public PixelmonParty(UUID owner, CompletableFuture<? extends PartyStorage> party) {
        super(TYPE, party);
        this.owner = owner;
    }

    /**
     * @return the UUID of the player who owns the party
     */
    @Override
    public UUID owner() {
        return this.owner;
    }

    /**
     * Gets wrappers for all Pokémon in the party. Empty slots are skipped.
     *
     * @return a new list of the Pokémon, which is empty if the party is not loaded
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
     * Checks if the party has an empty slot.
     *
     * @return {@code false} if the party is full or not loaded
     */
    @Override
    public boolean hasSpace() {
        return this.isLoaded() && this.dereference().hasSpace();
    }

    /**
     * Adds the Pokémon to the party.
     *
     * @param pokemon the Pokémon to add
     * @return {@code true} if Pixelmon added the Pokémon, or {@code false} if the party is not loaded
     */
    @Override
    public boolean add(UPokemon<?> pokemon) {
        return this.isLoaded() && this.dereference().add(PixelmonPokemon.unwrap(pokemon));
    }

    /**
     * Removes the Pokémon from its slot in the party.
     *
     * @param pokemon the Pokémon to remove
     * @return {@code false} if the party is not loaded or does not hold the Pokémon
     */
    @Override
    public boolean remove(UPokemon<?> pokemon) {
        if (!this.isLoaded()) {
            return false;
        }
        var party = this.dereference();
        var position = party.getPosition(PixelmonPokemon.unwrap(pokemon));
        if (position == null) {
            return false;
        }
        party.set(position, null);
        return true;
    }

    /**
     * Finds the Pokémon with the UUID in the party.
     *
     * @param uuid the UUID of the Pokémon
     * @return the Pokémon, or {@code null} if the party is not loaded or has no such Pokémon
     */
    @Override
    public @Nullable PixelmonPokemon find(UUID uuid) {
        return this.isLoaded() ? PixelmonPokemon.of(this.dereference().find(uuid)) : null;
    }

    /**
     * Gets the Pokémon in the slot.
     *
     * @param slot the index of the slot
     * @return the Pokémon, or {@code null} if the party is not loaded or the slot is empty
     */
    @Override
    public @Nullable PixelmonPokemon get(int slot) {
        return this.isLoaded() ? PixelmonPokemon.of(this.dereference().get(slot)) : null;
    }

    /**
     * Puts the Pokémon in the slot. Does nothing if the party is not loaded.
     *
     * @param slot the index of the slot
     * @param pokemon the Pokémon to put in the slot, or {@code null} to empty the slot
     * @return the Pokémon that was in the slot before, or {@code null} if there was none
     */
    @Override
    public @Nullable PixelmonPokemon set(int slot, @Nullable UPokemon<?> pokemon) {
        if (!this.isLoaded()) {
            return null;
        }
        var old = this.get(slot);
        this.dereference().set(slot, pokemon == null ? null : PixelmonPokemon.unwrap(pokemon));
        return old;
    }

    /**
     * Swaps the Pokémon in the two slots. Does nothing if the party is not loaded.
     *
     * @param slot1 the index of the first slot
     * @param slot2 the index of the second slot
     */
    @Override
    public void swap(int slot1, int slot2) {
        if (this.isLoaded()) {
            this.dereference().swap(slot1, slot2);
        }
    }

    /**
     * Heals all Pokémon in the party. Does nothing if the party is not loaded.
     */
    @Override
    public void heal() {
        if (this.isLoaded()) {
            this.dereference().heal();
        }
    }

    /**
     * Retrieves all sent-out Pokémon of the party, with the {@link PokemonRetrievedEvent.Constants#FORCED} cause.
     * Does nothing if the party is not loaded.
     */
    @Override
    public void retrieveAll() {
        if (this.isLoaded()) {
            this.dereference().retrieveAll(PokemonRetrievedEvent.Constants.FORCED);
        }
    }
}
