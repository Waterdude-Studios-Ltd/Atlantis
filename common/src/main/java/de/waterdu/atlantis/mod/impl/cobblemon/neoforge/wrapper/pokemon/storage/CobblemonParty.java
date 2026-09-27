package de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.storage;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UParty;

public class CobblemonParty extends UParty<PartyStore, CobblemonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "party");

    public CobblemonParty(PartyStore party) {
        super(TYPE, party);
    }

    @Override
    public CobblemonPokemon get(int slot) {
        return new CobblemonPokemon(this.dereference().get(slot));
    }

    @Override
    public CobblemonPokemon set(int slot, CobblemonPokemon pokemon) {
        var party = this.dereference();
        var old = new CobblemonPokemon(party.get(slot));
        party.set(slot, pokemon.dereference());
        return old;
    }
}
