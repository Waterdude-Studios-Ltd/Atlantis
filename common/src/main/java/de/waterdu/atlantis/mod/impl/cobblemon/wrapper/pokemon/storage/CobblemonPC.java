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

public class CobblemonPC extends UPC<PCStore, CobblemonPokemon> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "pc");

    public CobblemonPC(PCStore pc) {
        super(TYPE, pc);
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
    public @Nullable CobblemonPokemon get(int box, int slot) {
        return CobblemonPokemon.of(this.dereference().get(new PCPosition(box, slot)));
    }

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

    @Override
    public int boxCount() {
        return this.dereference().getBoxes().size();
    }

    @Override
    public int boxSize() {
        return ConstantsKt.POKEMON_PER_BOX;
    }
}
