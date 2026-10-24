package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UAbility;
import net.minecraft.network.chat.Component;

public class PixelmonAbility extends UAbility<Ability> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "ability");

    public PixelmonAbility(Ability ability) {
        super(TYPE, ability);
    }

    @Override
    public String name() {
        return this.dereference().getName();
    }

    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }
}
