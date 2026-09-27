package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UParty;

import java.util.concurrent.CompletableFuture;

public class PixelmonParty extends UParty<PartyStorage, PixelmonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "party");

    public PixelmonParty(CompletableFuture<? extends PartyStorage> party) {
        super(TYPE, party);
    }

    @Override
    public PixelmonPokemon get(int slot) {
        var party = this.dereference();
        if (party == null) {
            return null;
        }

        return new PixelmonPokemon(party.get(slot));
    }

    @Override
    public PixelmonPokemon set(int slot, PixelmonPokemon pokemon) {
        var party = this.dereference();
        if (party == null) {
            return null;
        }

        var old = new PixelmonPokemon(party.get(slot));
        party.set(slot, pokemon.dereference());
        return old;
    }
}
