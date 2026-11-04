package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a Tera type of a Pokémon mod.
 *
 * @param <T> the type of the Tera type object of the mod
 */
public abstract class UTeraType<T> extends ModWrapper<T> {

    public static final String STELLAR = "stellar";

    /**
     * Creates a wrapper for the Tera type.
     *
     * @param type the key of the mod object type
     * @param teraType the Tera type object of the mod
     */
    protected UTeraType(ModObjectKey type, T teraType) {
        super(new ModObject(type, teraType));
    }

    /**
     * @return the name of the Tera type
     */
    public abstract String name();

    /**
     * @return the name of the Tera type to show to players
     */
    public abstract Component displayName();

    /**
     * Checks if this is the Stellar Tera type.
     *
     * @return {@code true} for Stellar
     */
    public abstract boolean isStellar();

    /**
     * Gets the elemental type that this Tera type changes the Pokémon into.
     *
     * @return the elemental type, or {@code null} for Stellar
     */
    public abstract @Nullable UElementalType<?> elementalType();

    /**
     * Checks if this Tera type has the same name as the other Tera type. The case of the names is ignored.
     *
     * @param type the Tera type to compare with
     * @return {@code true} if the names are equal
     */
    public boolean is(UTeraType<?> type) {
        return this.name().equalsIgnoreCase(type.name());
    }

    /**
     * Checks if this Tera type changes the Pokémon into the elemental type.
     *
     * @param type the elemental type to compare with
     * @return {@code true} if the elemental type of this Tera type is the same type. Stellar always gives
     *     {@code false}.
     */
    public boolean is(UElementalType<?> type) {
        var elementalType = this.elementalType();
        return elementalType != null && elementalType.is(type);
    }
}
