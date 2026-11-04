package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Nature;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats.CobblemonStat;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UNature;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Wraps a Cobblemon nature.
 */
public class CobblemonNature extends UNature<Nature> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "nature");

    /**
     * Creates a wrapper for a Cobblemon nature.
     *
     * @param nature the nature to wrap
     */
    public CobblemonNature(Nature nature) {
        super(TYPE, nature);
    }

    /**
     * @return the path of the resource location of the nature
     */
    @Override
    public String name() {
        return this.dereference().getName().getPath();
    }

    /**
     * @return a translatable component made from the display name key of the nature
     */
    @Override
    public Component displayName() {
        return Component.translatable(this.dereference().getDisplayName());
    }

    /**
     * Gets the stat that the nature increases.
     *
     * @return the increased stat, or {@code null} for a neutral nature
     */
    @Override
    public @Nullable CobblemonStat increased() {
        var stat = this.dereference().getIncreasedStat();
        return stat == null ? null : new CobblemonStat(stat);
    }

    /**
     * Gets the stat that the nature decreases.
     *
     * @return the decreased stat, or {@code null} for a neutral nature
     */
    @Override
    public @Nullable CobblemonStat decreased() {
        var stat = this.dereference().getDecreasedStat();
        return stat == null ? null : new CobblemonStat(stat);
    }
}
