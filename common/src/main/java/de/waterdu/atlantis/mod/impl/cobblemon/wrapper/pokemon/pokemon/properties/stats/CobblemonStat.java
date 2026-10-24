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

public class CobblemonStat extends UStat<Stat> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "stat");

    public CobblemonStat(Stat stat) {
        super(TYPE, stat);
    }

    public static @Nullable CobblemonStat of(String name) {
        for (var stat : Stats.values()) {
            if (stat.name().equalsIgnoreCase(name)) {
                return new CobblemonStat(stat);
            }
        }
        return null;
    }

    public static List<CobblemonStat> all() {
        var stats = new ArrayList<CobblemonStat>();
        for (var stat : Stats.values()) {
            stats.add(new CobblemonStat(stat));
        }
        return stats;
    }

    @Override
    public String name() {
        var stat = this.dereference();
        return stat instanceof Stats stats ? stats.name().toLowerCase(Locale.ROOT) : stat.getIdentifier().getPath();
    }

    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }
}
