package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.FormData;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps Cobblemon form data.
 */
public class CobblemonForm extends UForm<FormData> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "form");

    /**
     * Creates a wrapper for Cobblemon form data.
     *
     * @param form the form data to wrap
     */
    public CobblemonForm(FormData form) {
        super(TYPE, form);
    }

    /**
     * @return the Cobblemon name of the form
     */
    @Override
    public String name() {
        return this.dereference().getName();
    }

    /**
     * @return a new wrapper for the species of the form
     */
    @Override
    public CobblemonSpecies species() {
        return new CobblemonSpecies(this.dereference().getSpecies());
    }

    /**
     * Gets a new wrapper for each elemental type of the form.
     *
     * @return the elemental types of the form
     */
    @Override
    public List<CobblemonElementalType> types() {
        var types = new ArrayList<CobblemonElementalType>();
        for (var type : this.dereference().getTypes()) {
            types.add(new CobblemonElementalType(type));
        }
        return types;
    }

    /**
     * Gets the base value of a stat for the form.
     *
     * @param stat the stat to get
     * @return the base value, or {@code 0} if the stat is not permanent or the form has no value for it
     */
    @Override
    public int baseStat(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getBaseStats().getOrDefault((Stat) stat.dereference(), 0) : 0;
    }

    /**
     * Adds up all base stats of the form.
     *
     * @return the base stat total
     */
    @Override
    public int baseStatTotal() {
        var total = 0;
        for (var value : this.dereference().getBaseStats().values()) {
            total += value;
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
