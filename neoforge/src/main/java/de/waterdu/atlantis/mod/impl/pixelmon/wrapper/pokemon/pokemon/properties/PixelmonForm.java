package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;

import java.util.List;

/**
 * Wraps a Pixelmon form.
 */
public class PixelmonForm extends UForm<Stats> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "form");

    /**
     * Creates a wrapper for a Pixelmon form.
     *
     * @param form the form to wrap
     */
    public PixelmonForm(Stats form) {
        super(TYPE, form);
    }

    /**
     * @return the Pixelmon name of the form
     */
    @Override
    public String name() {
        return this.dereference().getName();
    }

    /**
     * @return a new wrapper for the species that the form belongs to
     */
    @Override
    public PixelmonSpecies species() {
        return new PixelmonSpecies(this.dereference().getParentSpecies());
    }

    /**
     * @return new wrappers for the elemental types of the form
     */
    @Override
    public List<PixelmonElementalType> types() {
        return this.dereference().getTypes().stream().map(PixelmonElementalType::new).toList();
    }

    /**
     * Gets the base value of a stat of the form.
     *
     * @param stat the stat to get
     * @return the base value, or {@code 0} if the stat is not permanent
     */
    @Override
    public int baseStat(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getBattleStats().getStat((BattleStatsType) stat.dereference()) : 0;
    }

    /**
     * Adds the base values of all EV and IV stats of the form.
     *
     * @return the base stat total
     */
    @Override
    public int baseStatTotal() {
        var total = 0;
        for (var stat : BattleStatsType.getEVIVStatValues()) {
            total += this.dereference().getBattleStats().getStat(stat);
        }
        return total;
    }

    /**
     * @return the catch rate of the form
     */
    @Override
    public int catchRate() {
        return this.dereference().getCatchRate();
    }
}
