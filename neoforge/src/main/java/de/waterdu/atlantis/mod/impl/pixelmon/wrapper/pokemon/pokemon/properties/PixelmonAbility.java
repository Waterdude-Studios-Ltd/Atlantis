package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UAbility;
import net.minecraft.network.chat.Component;

/**
 * Wraps a Pixelmon ability.
 */
public class PixelmonAbility extends UAbility<Ability> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "ability");

    /**
     * Creates a wrapper for a Pixelmon ability.
     *
     * @param ability the ability to wrap
     */
    public PixelmonAbility(Ability ability) {
        super(TYPE, ability);
    }

    /**
     * @return the Pixelmon name of the ability
     */
    @Override
    public String name() {
        return this.dereference().getName();
    }

    /**
     * @return the translated name of the ability
     */
    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }
}
