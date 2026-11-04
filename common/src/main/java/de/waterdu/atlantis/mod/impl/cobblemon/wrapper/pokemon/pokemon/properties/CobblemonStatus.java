package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.status.Statuses;
import com.cobblemon.mod.common.pokemon.status.PersistentStatus;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UStatus;

import java.util.List;

/**
 * Wraps a Cobblemon persistent status, such as burn or sleep.
 */
public class CobblemonStatus extends UStatus<PersistentStatus> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "status");

    /**
     * Creates a wrapper for a Cobblemon persistent status.
     *
     * @param status the Cobblemon status to wrap
     */
    public CobblemonStatus(PersistentStatus status) {
        super(TYPE, status);
    }

    /**
     * Gets the six main persistent statuses of Cobblemon.
     *
     * @return an immutable list with burn, freeze, paralysis, poison, bad poison and sleep
     */
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

    /**
     * Gets the name of this status. Frozen and badly poisoned map to {@link #FREEZE} and {@link #BAD_POISON}. Other
     * statuses use the path of their Cobblemon identifier.
     *
     * @return the name of the status
     */
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
