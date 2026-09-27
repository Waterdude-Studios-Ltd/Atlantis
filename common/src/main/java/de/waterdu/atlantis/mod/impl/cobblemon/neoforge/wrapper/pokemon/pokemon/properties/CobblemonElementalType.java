package de.waterdu.atlantis.mod.impl.cobblemon.neoforge.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.types.ElementalType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;

public class CobblemonElementalType extends UElementalType<ElementalType> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "elemental_type");

    public CobblemonElementalType(ElementalType elementalType) {
        super(TYPE, elementalType);
    }
}
