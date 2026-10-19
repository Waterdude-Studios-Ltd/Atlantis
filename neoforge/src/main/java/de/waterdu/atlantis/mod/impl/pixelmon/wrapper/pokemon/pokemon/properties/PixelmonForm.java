package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;

import java.util.List;

public class PixelmonForm extends UForm<Stats> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "form");

    public PixelmonForm(Stats form) {
        super(TYPE, form);
    }

    @Override
    public List<PixelmonElementalType> types() {
        return this.dereference().getTypes().stream().map(it -> new PixelmonElementalType(it.value())).toList();
    }
}
