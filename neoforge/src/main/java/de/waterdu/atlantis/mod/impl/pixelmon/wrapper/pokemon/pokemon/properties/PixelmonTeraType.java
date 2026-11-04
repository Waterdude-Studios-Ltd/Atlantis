package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.type.Type;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UTeraType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Wraps a Pixelmon Tera type.
 */
public class PixelmonTeraType extends UTeraType<Holder<Type>> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "tera_type");

    /**
     * Creates a wrapper for a Pixelmon Tera type.
     *
     * @param teraType the holder of the type to wrap
     */
    public PixelmonTeraType(Holder<Type> teraType) {
        super(TYPE, teraType);
    }

    /**
     * Gets the registry path of the type, in lower case. If the holder has no key, the display name is used.
     *
     * @return the name of the Tera type
     */
    @Override
    public String name() {
        return this.dereference().unwrapKey()
                .map(key -> key.location().getPath())
                .orElseGet(() -> this.displayName().getString())
                .toLowerCase(Locale.ROOT);
    }

    /**
     * @return the display name of the Tera type
     */
    @Override
    public Component displayName() {
        return this.dereference().value().name();
    }

    /**
     * Checks if the Tera type is Stellar.
     *
     * @return {@code true} if the Tera type is {@link Type#STELLAR}
     */
    @Override
    public boolean isStellar() {
        return this.dereference().is(Type.STELLAR);
    }

    /**
     * Gets the elemental type that this Tera type changes the Pokémon into.
     *
     * @return the elemental type, or {@code null} for Stellar
     */
    @Override
    public @Nullable PixelmonElementalType elementalType() {
        return this.isStellar() ? null : new PixelmonElementalType(this.dereference());
    }
}
