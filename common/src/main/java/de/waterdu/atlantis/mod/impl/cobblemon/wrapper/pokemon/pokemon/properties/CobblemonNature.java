package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Nature;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.stats.CobblemonStat;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UNature;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class CobblemonNature extends UNature<Nature> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "nature");

    public CobblemonNature(Nature nature) {
        super(TYPE, nature);
    }

    @Override
    public String name() {
        return this.dereference().getName().getPath();
    }

    @Override
    public Component displayName() {
        return Component.translatable(this.dereference().getDisplayName());
    }

    @Override
    public @Nullable CobblemonStat increased() {
        var stat = this.dereference().getIncreasedStat();
        return stat == null ? null : new CobblemonStat(stat);
    }

    @Override
    public @Nullable CobblemonStat decreased() {
        var stat = this.dereference().getDecreasedStat();
        return stat == null ? null : new CobblemonStat(stat);
    }
}
