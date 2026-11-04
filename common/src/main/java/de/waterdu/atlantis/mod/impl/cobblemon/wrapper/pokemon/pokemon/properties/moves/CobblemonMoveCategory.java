package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.categories.DamageCategory;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveCategory;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Wraps a Cobblemon damage category, such as physical, special or status.
 */
public class CobblemonMoveCategory extends UMoveCategory<DamageCategory> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "move_category");

    /**
     * Creates a wrapper for a Cobblemon damage category.
     *
     * @param category the Cobblemon damage category to wrap
     */
    public CobblemonMoveCategory(DamageCategory category) {
        super(TYPE, category);
    }

    /**
     * Gets the Cobblemon name of this category, in lower case.
     *
     * @return the name of the category
     */
    @Override
    public String name() {
        return this.dereference().getName().toLowerCase(Locale.ROOT);
    }

    /**
     * @return the Cobblemon display name of the category
     */
    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }
}
