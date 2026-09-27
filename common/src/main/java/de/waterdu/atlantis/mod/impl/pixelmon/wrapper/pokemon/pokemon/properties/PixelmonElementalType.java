package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.type.Type;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;

public class PixelmonElementalType extends UElementalType<Type> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "elemental_type");

    public PixelmonElementalType(Type elementalType) {
        super(TYPE, elementalType);
    }
}
