package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.USpecies;

public class PixelmonSpecies extends USpecies<Species> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "species");

    public PixelmonSpecies(Species instance) {
        super(TYPE, instance);
    }

    @Override
    public String name() {
        return this.dereference().getTranslatedName().getString();
    }

    @Override
    public int dex() {
        return this.dereference().getDex();
    }
}
