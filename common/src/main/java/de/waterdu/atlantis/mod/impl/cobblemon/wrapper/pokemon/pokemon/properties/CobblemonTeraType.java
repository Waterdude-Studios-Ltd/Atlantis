package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.elemental.ElementalTypeTeraType;
import com.cobblemon.mod.common.api.types.tera.gimmick.StellarTeraType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UTeraType;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Wraps a Cobblemon Tera type.
 */
public class CobblemonTeraType extends UTeraType<TeraType> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "tera_type");

    /**
     * Creates a wrapper for a Cobblemon Tera type.
     *
     * @param teraType the Cobblemon Tera type to wrap
     */
    public CobblemonTeraType(TeraType teraType) {
        super(TYPE, teraType);
    }

    /**
     * Gets the Cobblemon name of this Tera type, in lower case.
     *
     * @return the name of the Tera type
     */
    @Override
    public String name() {
        return this.dereference().getName().toLowerCase(Locale.ROOT);
    }

    /**
     * @return the Cobblemon display name of the Tera type
     */
    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }

    /**
     * Checks if this Tera type is the Cobblemon Stellar Tera type.
     *
     * @return {@code true} for Stellar
     */
    @Override
    public boolean isStellar() {
        return this.dereference() instanceof StellarTeraType;
    }

    /**
     * Gets the elemental type that this Tera type changes the Pokémon into.
     *
     * @return the elemental type, or {@code null} if this Tera type has no elemental type, such as Stellar
     */
    @Override
    public @Nullable CobblemonElementalType elementalType() {
        return this.dereference() instanceof ElementalTypeTeraType teraType ? new CobblemonElementalType(teraType.getType()) : null;
    }
}
