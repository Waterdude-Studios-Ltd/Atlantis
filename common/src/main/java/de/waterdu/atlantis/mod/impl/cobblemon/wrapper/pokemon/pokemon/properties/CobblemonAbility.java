package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UAbility;
import net.minecraft.network.chat.Component;

/**
 * Wraps a Cobblemon ability template.
 */
public class CobblemonAbility extends UAbility<AbilityTemplate> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "ability");

    /**
     * Creates a wrapper for a Cobblemon ability template.
     *
     * @param ability the ability template to wrap
     */
    public CobblemonAbility(AbilityTemplate ability) {
        super(TYPE, ability);
    }

    /**
     * @return the Cobblemon name of the ability
     */
    @Override
    public String name() {
        return this.dereference().getName();
    }

    /**
     * @return a translatable component made from the display name key of the ability
     */
    @Override
    public Component displayName() {
        return Component.translatable(this.dereference().getDisplayName());
    }
}
