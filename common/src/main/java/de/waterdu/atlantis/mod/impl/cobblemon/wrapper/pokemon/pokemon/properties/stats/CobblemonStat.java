package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wraps a Cobblemon stat.
 */
public class CobblemonStat extends UStat<Stat> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "stat");

    /**
     * Creates a wrapper for a Cobblemon stat.
     *
     * @param stat the Cobblemon stat to wrap
     */
    public CobblemonStat(Stat stat) {
        super(TYPE, stat);
    }

    /**
     * Finds the Cobblemon stat with the given name. The name comparison ignores case.
     *
     * @param name the name of a {@link Stats} constant
     * @return the stat, or {@code null} if no stat has that name
     */
    public static @Nullable CobblemonStat of(String name) {
        for (var stat : Stats.values()) {
            if (stat.name().equalsIgnoreCase(name)) {
                return new CobblemonStat(stat);
            }
        }
        return null;
    }

    /**
     * Gets all stats in {@link Stats}, including the battle-only stats.
     *
     * @return a new list of the stats
     */
    public static List<CobblemonStat> all() {
        var stats = new ArrayList<CobblemonStat>();
        for (var stat : Stats.values()) {
            stats.add(new CobblemonStat(stat));
        }
        return stats;
    }

    /**
     * Gets the name of this stat. A {@link Stats} constant gives its name in lower case. Other stats give the path of
     * their identifier.
     *
     * @return the name of the stat
     */
    @Override
    public String name() {
        var stat = this.dereference();
        return stat instanceof Stats stats ? stats.name().toLowerCase(Locale.ROOT) : stat.getIdentifier().getPath();
    }

    /**
     * @return the Cobblemon display name of the stat
     */
    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }
}
