package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.stats;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wraps a Pixelmon {@link BattleStatsType}.
 */
public class PixelmonStat extends UStat<BattleStatsType> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "stat");

    /**
     * Creates a wrapper for the Pixelmon stat.
     *
     * @param stat the Pixelmon stat type
     */
    public PixelmonStat(BattleStatsType stat) {
        super(TYPE, stat);
    }

    /**
     * Finds the stat with the name, ignoring case.
     *
     * @param name the name of the stat, such as {@link #SPECIAL_DEFENCE}
     * @return the stat, or {@code null} if no stat has the name
     */
    public static @Nullable PixelmonStat of(String name) {
        for (var stat : all()) {
            if (stat.name().equalsIgnoreCase(name)) {
                return stat;
            }
        }
        return null;
    }

    /**
     * Gets wrappers for all Pixelmon stats, except {@link BattleStatsType#NONE}.
     *
     * @return a new list of the stats
     */
    public static List<PixelmonStat> all() {
        var stats = new ArrayList<PixelmonStat>();
        for (var stat : BattleStatsType.values()) {
            if (stat != BattleStatsType.NONE) {
                stats.add(new PixelmonStat(stat));
            }
        }
        return stats;
    }

    /**
     * Gets the name of the stat, in lower case. Defense stats use the {@link #DEFENCE} spelling.
     *
     * @return the name of the stat
     */
    @Override
    public String name() {
        return switch (this.dereference()) {
            case DEFENSE -> DEFENCE;
            case SPECIAL_DEFENSE -> SPECIAL_DEFENCE;
            default -> this.dereference().name().toLowerCase(Locale.ROOT);
        };
    }

    /**
     * @return the translated name of the stat
     */
    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }
}
