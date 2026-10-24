package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UGender;

public class PixelmonGender extends UGender<Gender> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "gender");

    public PixelmonGender(Gender gender) {
        super(TYPE, gender);
    }

    @Override
    public String name() {
        return switch (this.dereference()) {
            case MALE -> MALE;
            case FEMALE -> FEMALE;
            case NONE -> GENDERLESS;
        };
    }
}
