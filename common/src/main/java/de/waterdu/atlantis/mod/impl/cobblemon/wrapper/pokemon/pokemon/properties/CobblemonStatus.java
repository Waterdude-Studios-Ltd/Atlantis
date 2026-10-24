package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.status.Statuses;
import com.cobblemon.mod.common.pokemon.status.PersistentStatus;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UStatus;

import java.util.List;

public class CobblemonStatus extends UStatus<PersistentStatus> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "status");

    public CobblemonStatus(PersistentStatus status) {
        super(TYPE, status);
    }

    public static List<CobblemonStatus> all() {
        return List.of(
                new CobblemonStatus(Statuses.BURN),
                new CobblemonStatus(Statuses.FROZEN),
                new CobblemonStatus(Statuses.PARALYSIS),
                new CobblemonStatus(Statuses.POISON),
                new CobblemonStatus(Statuses.POISON_BADLY),
                new CobblemonStatus(Statuses.SLEEP)
        );
    }

    @Override
    public String name() {
        var status = this.dereference();
        if (status == Statuses.FROZEN) {
            return FREEZE;
        } else if (status == Statuses.POISON_BADLY) {
            return BAD_POISON;
        }
        return status.getName().getPath();
    }
}
