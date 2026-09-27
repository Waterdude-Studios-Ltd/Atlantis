package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.storage;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.PixelmonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;

import java.util.concurrent.CompletableFuture;

public class PixelmonPC extends UPC<PCStorage, PixelmonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "pc");

    public PixelmonPC(CompletableFuture<PCStorage> pc) {
        super(TYPE, pc);
    }

    @Override
    public PixelmonPokemon get(int box, int slot) {
        var pc = this.dereference();
        if (pc == null) {
            return null;
        }

        return new PixelmonPokemon(pc.get(box, slot));
    }

    @Override
    public PixelmonPokemon set(PixelmonPokemon pokemon, int box, int slot) {
        var pc = this.dereference();
        if (pc == null) {
            return null;
        }

        var old = new PixelmonPokemon(pc.get(box, slot));
        pc.set(box, slot, pokemon.dereference());
        return old;
    }
}
