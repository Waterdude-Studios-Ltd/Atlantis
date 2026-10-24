package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveCategory;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class CobblemonMoveCategory extends UMoveCategory<DamageCategory> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "move_category");

    public CobblemonMoveCategory(DamageCategory category) {
        super(TYPE, category);
    }

    @Override
    public String name() {
        return this.dereference().getName().toLowerCase(Locale.ROOT);
    }

    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }
}
