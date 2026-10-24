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

public class PixelmonStat extends UStat<BattleStatsType> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "stat");

    public PixelmonStat(BattleStatsType stat) {
        super(TYPE, stat);
    }

    public static @Nullable PixelmonStat of(String name) {
        for (var stat : all()) {
            if (stat.name().equalsIgnoreCase(name)) {
                return stat;
            }
        }
        return null;
    }

    public static List<PixelmonStat> all() {
        var stats = new ArrayList<PixelmonStat>();
        for (var stat : BattleStatsType.values()) {
            if (stat != BattleStatsType.NONE) {
                stats.add(new PixelmonStat(stat));
            }
        }
        return stats;
    }

    @Override
    public String name() {
        return switch (this.dereference()) {
            case DEFENSE -> DEFENCE;
            case SPECIAL_DEFENSE -> SPECIAL_DEFENCE;
            default -> this.dereference().name().toLowerCase(Locale.ROOT);
        };
    }

    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }
}
