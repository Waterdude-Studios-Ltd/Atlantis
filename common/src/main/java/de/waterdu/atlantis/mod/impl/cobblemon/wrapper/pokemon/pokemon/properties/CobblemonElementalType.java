package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.types.ElementalType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Wraps a Cobblemon elemental type.
 */
public class CobblemonElementalType extends UElementalType<ElementalType> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "elemental_type");

    /**
     * Creates a wrapper for a Cobblemon elemental type.
     *
     * @param elementalType the elemental type to wrap
     */
    public CobblemonElementalType(ElementalType elementalType) {
        super(TYPE, elementalType);
    }

    /**
     * @return the Cobblemon name of the type, in lower case
     */
    @Override
    public String name() {
        return this.dereference().getName().toLowerCase(Locale.ROOT);
    }

    /**
     * @return the display name of the type
     */
    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }
}
