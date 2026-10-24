package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UAbility;
import net.minecraft.network.chat.Component;

public class CobblemonAbility extends UAbility<AbilityTemplate> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "ability");

    public CobblemonAbility(AbilityTemplate ability) {
        super(TYPE, ability);
    }

    @Override
    public String name() {
        return this.dereference().getName();
    }

    @Override
    public Component displayName() {
        return Component.translatable(this.dereference().getDisplayName());
    }
}
