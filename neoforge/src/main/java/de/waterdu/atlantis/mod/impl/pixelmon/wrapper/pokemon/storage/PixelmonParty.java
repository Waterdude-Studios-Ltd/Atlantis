package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage;

import com.pixelmonmod.pixelmon.Pixelmon;
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

public class PixelmonParty extends UParty<PartyStorage, PixelmonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "party");

    private final UUID owner;

    public PixelmonParty(UUID owner, CompletableFuture<? extends PartyStorage> party) {
        super(TYPE, party);
        this.owner = owner;
    }

    @Override
    public UUID owner() {
        return this.owner;
    }

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

    @Override
    public boolean hasSpace() {
        return this.isLoaded() && this.dereference().hasSpace();
    }

    @Override
    public boolean add(UPokemon<?> pokemon) {
        return this.isLoaded() && this.dereference().add(PixelmonPokemon.unwrap(pokemon));
    }

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

    @Override
    public @Nullable PixelmonPokemon find(UUID uuid) {
        return this.isLoaded() ? PixelmonPokemon.of(this.dereference().find(uuid)) : null;
    }

    @Override
    public @Nullable PixelmonPokemon get(int slot) {
        return this.isLoaded() ? PixelmonPokemon.of(this.dereference().get(slot)) : null;
    }

    @Override
    public @Nullable PixelmonPokemon set(int slot, @Nullable UPokemon<?> pokemon) {
        if (!this.isLoaded()) {
            return null;
        }
        var old = this.get(slot);
        this.dereference().set(slot, pokemon == null ? null : PixelmonPokemon.unwrap(pokemon));
        return old;
    }

    @Override
    public void swap(int slot1, int slot2) {
        if (this.isLoaded()) {
            this.dereference().swap(slot1, slot2);
        }
    }

    @Override
    public void heal() {
        if (this.isLoaded()) {
            this.dereference().heal();
        }
    }
}
