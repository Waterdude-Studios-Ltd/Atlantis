package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.FormData;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;

import java.util.ArrayList;
import java.util.List;

public class CobblemonForm extends UForm<FormData> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "form");

    public CobblemonForm(FormData form) {
        super(TYPE, form);
    }

    @Override
    public List<CobblemonElementalType> types() {
        var types = new ArrayList<CobblemonElementalType>();
        for (var type : this.dereference().getTypes()) {
            types.add(new CobblemonElementalType(type));
        }
        return types;
    }
}
