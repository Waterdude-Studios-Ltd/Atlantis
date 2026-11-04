package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Gender;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UGender;

import java.util.Locale;

/**
 * Wraps a Cobblemon gender.
 */
public class CobblemonGender extends UGender<Gender> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "gender");

    /**
     * Creates a wrapper for a Cobblemon gender.
     *
     * @param gender the gender to wrap
     */
    public CobblemonGender(Gender gender) {
        super(TYPE, gender);
    }

    /**
     * @return the enum name of the gender, in lower case
     */
    @Override
    public String name() {
        return this.dereference().name().toLowerCase(Locale.ROOT);
    }
}
