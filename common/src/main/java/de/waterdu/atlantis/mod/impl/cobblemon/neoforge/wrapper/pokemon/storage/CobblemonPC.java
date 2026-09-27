package de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.storage;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.pokemon.CobblemonPokemon;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.storage.UPC;

public class CobblemonPC extends UPC<PCStore, CobblemonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pc");

    public CobblemonPC(PCStore pc) {
        super(TYPE, pc);
    }

    @Override
    public CobblemonPokemon get(int box, int slot) {
        return new CobblemonPokemon(this.dereference().get(new PCPosition(box, slot)));
    }

    @Override
    public CobblemonPokemon set(CobblemonPokemon pokemon, int box, int slot) {
        var pc = this.dereference();
        var position = new PCPosition(box, slot);
        var old = new CobblemonPokemon(pc.get(position));
        pc.set(position, pokemon.dereference());
        return old;
    }
}
