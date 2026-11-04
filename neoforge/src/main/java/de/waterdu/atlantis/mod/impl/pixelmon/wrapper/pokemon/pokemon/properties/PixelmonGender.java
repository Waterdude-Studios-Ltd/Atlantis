package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UGender;

/**
 * Wraps a Pixelmon gender.
 */
public class PixelmonGender extends UGender<Gender> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "gender");

    /**
     * Creates a wrapper for a Pixelmon gender.
     *
     * @param gender the gender to wrap
     */
    public PixelmonGender(Gender gender) {
        super(TYPE, gender);
    }

    /**
     * Gets the name of the gender. {@link Gender#NONE} gives {@link #GENDERLESS}.
     *
     * @return the name of the gender
     */
    @Override
    public String name() {
        return switch (this.dereference()) {
            case MALE -> MALE;
            case FEMALE -> FEMALE;
            case NONE -> GENDERLESS;
        };
    }
}
