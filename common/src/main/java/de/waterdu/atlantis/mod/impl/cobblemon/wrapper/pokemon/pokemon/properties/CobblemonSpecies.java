package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Species;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;

public class CobblemonSpecies extends USpecies<Species> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "species");

    public CobblemonSpecies(Species instance) {
        super(TYPE, instance);
    }

    @Override
    public String name() {
        return this.dereference().getTranslatedName().getString();
    }

    @Override
    public int dex() {
        return this.dereference().getNationalPokedexNumber();
    }
}
