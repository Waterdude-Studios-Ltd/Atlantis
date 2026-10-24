package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.FormData;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UForm;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UStat;

import java.util.ArrayList;
import java.util.List;

public class CobblemonForm extends UForm<FormData> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "form");

    public CobblemonForm(FormData form) {
        super(TYPE, form);
    }

    @Override
    public String name() {
        return this.dereference().getName();
    }

    @Override
    public CobblemonSpecies species() {
        return new CobblemonSpecies(this.dereference().getSpecies());
    }

    @Override
    public List<CobblemonElementalType> types() {
        var types = new ArrayList<CobblemonElementalType>();
        for (var type : this.dereference().getTypes()) {
            types.add(new CobblemonElementalType(type));
        }
        return types;
    }

    @Override
    public int baseStat(UStat<?> stat) {
        return stat.isPermanent() ? this.dereference().getBaseStats().getOrDefault((Stat) stat.dereference(), 0) : 0;
    }

    @Override
    public int baseStatTotal() {
        var total = 0;
        for (var value : this.dereference().getBaseStats().values()) {
            total += value;
        }
        return total;
    }

    @Override
    public int catchRate() {
        return this.dereference().getCatchRate();
    }
}
