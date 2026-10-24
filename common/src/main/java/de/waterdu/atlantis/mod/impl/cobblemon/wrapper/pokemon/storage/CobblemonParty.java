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

public class CobblemonParty extends UParty<PartyStore, CobblemonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "party");

    public CobblemonParty(PartyStore party) {
        super(TYPE, party);
    }

    @Override
    public UUID owner() {
        return this.dereference().getUuid();
    }

    @Override
    public List<CobblemonPokemon> all() {
        var all = new ArrayList<CobblemonPokemon>();
        for (var pokemon : this.dereference()) {
            all.add(new CobblemonPokemon(pokemon));
        }
        return all;
    }

    @Override
    public boolean hasSpace() {
        return this.dereference().getFirstAvailablePosition() != null;
    }

    @Override
    public boolean add(UPokemon<?> pokemon) {
        return this.dereference().add(CobblemonPokemon.unwrap(pokemon));
    }

    @Override
    public boolean remove(UPokemon<?> pokemon) {
        return this.dereference().remove(CobblemonPokemon.unwrap(pokemon));
    }

    @Override
    public @Nullable CobblemonPokemon find(UUID uuid) {
        return CobblemonPokemon.of(this.dereference().get(uuid));
    }

    @Override
    public @Nullable CobblemonPokemon get(int slot) {
        return CobblemonPokemon.of(this.dereference().get(slot));
    }

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

    @Override
    public void swap(int slot1, int slot2) {
        this.dereference().swap(slot1, slot2);
    }

    @Override
    public void heal() {
        this.dereference().heal();
    }
}
