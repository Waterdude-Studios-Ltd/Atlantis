package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;

import java.util.List;

public class PixelmonForm extends UForm<Stats> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "form");

    public PixelmonForm(Stats form) {
        super(TYPE, form);
    }

    @Override
    public String name() {
        return this.dereference().getName();
    }

    @Override
    public PixelmonSpecies species() {
        return new PixelmonSpecies(this.dereference().getParentSpecies());
    }

    @Override
    public List<PixelmonElementalType> types() {
        return this.dereference().getTypes().stream().map(PixelmonElementalType::new).toList();
    }

    @Override
    public int baseStat(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getBattleStats().getStat((BattleStatsType) stat.dereference()) : 0;
    }

    @Override
    public int baseStatTotal() {
        var total = 0;
        for (var stat : BattleStatsType.getEVIVStatValues()) {
            total += this.dereference().getBattleStats().getStat(stat);
        }
        return total;
    }

    @Override
    public int catchRate() {
        return this.dereference().getCatchRate();
    }
}
