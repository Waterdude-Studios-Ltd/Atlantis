package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Gender;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UGender;

import java.util.Locale;

public class CobblemonGender extends UGender<Gender> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "gender");

    public CobblemonGender(Gender gender) {
        super(TYPE, gender);
    }

    @Override
    public String name() {
        return this.dereference().name().toLowerCase(Locale.ROOT);
    }
}
