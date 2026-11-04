package de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats;

import de.waterdu.atlantis.mod.object.ModObject;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.ModWrapper;
import net.minecraft.network.chat.Component;

/**
 * Wraps a stat of a Pokémon mod, such as HP or speed.
 *
 * @param <T> the type of the stat object of the mod
 */
public abstract class UStat<T> extends ModWrapper<T> {

    public static final String HP = "hp";
    public static final String ATTACK = "attack";
    public static final String DEFENCE = "defence";
    public static final String SPECIAL_ATTACK = "special_attack";
    public static final String SPECIAL_DEFENCE = "special_defence";
    public static final String SPEED = "speed";
    public static final String ACCURACY = "accuracy";
    public static final String EVASION = "evasion";

    /**
     * Creates a wrapper for the stat.
     *
     * @param type the key of the mod object type
     * @param stat the stat object of the mod
     */
    protected UStat(ModObjectKey type, T stat) {
        super(new ModObject(type, stat));
    }

    /**
     * @return the name of the stat, such as {@link #HP}
     */
    public abstract String name();

    /**
     * @return the name of the stat to show to players
     */
    public abstract Component displayName();

    /**
     * @return {@code false} for {@link #ACCURACY} and {@link #EVASION}, which exist only in battle
     */
    public boolean isPermanent() {
        return !this.name().equals(ACCURACY) && !this.name().equals(EVASION);
    }

    /**
     * Checks if this stat has the same name as the other stat.
     *
     * @param stat the stat to compare with
     * @return {@code true} if the names are equal
     */
    public boolean is(UStat<?> stat) {
        return this.name().equals(stat.name());
    }
}
