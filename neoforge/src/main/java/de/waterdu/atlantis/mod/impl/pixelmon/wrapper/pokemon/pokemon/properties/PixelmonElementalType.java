package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.type.Type;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UElementalType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Wraps a Pixelmon elemental type.
 */
public class PixelmonElementalType extends UElementalType<Holder<Type>> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "elemental_type");

    /**
     * Creates a wrapper for a Pixelmon elemental type.
     *
     * @param elementalType the holder of the type to wrap
     */
    public PixelmonElementalType(Holder<Type> elementalType) {
        super(TYPE, elementalType);
    }

    /**
     * Gets the registry path of the type, in lower case. If the holder has no key, the display name is used.
     *
     * @return the name of the type
     */
    @Override
    public String name() {
        return this.dereference().unwrapKey()
                .map(key -> key.location().getPath())
                .orElseGet(() -> this.displayName().getString())
                .toLowerCase(Locale.ROOT);
    }

    /**
     * @return the display name of the type
     */
    @Override
    public Component displayName() {
        return this.dereference().value().name();
    }
}
