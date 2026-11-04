package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonElementalType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveTemplate;
import net.minecraft.network.chat.Component;

/**
 * Wraps a Cobblemon move template, which holds the shared data of a move.
 */
public class CobblemonMoveTemplate extends UMoveTemplate<MoveTemplate> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "move_template");

    /**
     * Creates a wrapper for a Cobblemon move template.
     *
     * @param move the Cobblemon move template to wrap
     */
    public CobblemonMoveTemplate(MoveTemplate move) {
        super(TYPE, move);
    }

    /**
     * @return the Cobblemon name of the move
     */
    @Override
    public String name() {
        return this.dereference().getName();
    }

    /**
     * @return the Cobblemon display name of the move
     */
    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }

    /**
     * @return a new wrapper for the elemental type of the move
     */
    @Override
    public CobblemonElementalType elementalType() {
        return new CobblemonElementalType(this.dereference().getElementalType());
    }

    /**
     * @return a new wrapper for the damage category of the move
     */
    @Override
    public CobblemonMoveCategory category() {
        return new CobblemonMoveCategory(this.dereference().getDamageCategory());
    }

    /**
     * Gets the base power of this move. The Cobblemon value is cut to an integer.
     *
     * @return the base power of the move
     */
    @Override
    public int power() {
        return (int) this.dereference().getPower();
    }

    /**
     * Gets the accuracy of this move. The Cobblemon value is cut to an integer.
     *
     * @return the accuracy of the move
     */
    @Override
    public int accuracy() {
        return (int) this.dereference().getAccuracy();
    }

    /**
     * @return the PP of the move without PP Ups
     */
    @Override
    public int basePP() {
        return this.dereference().getPp();
    }

    /**
     * @return the Cobblemon maximum PP of the move
     */
    @Override
    public int maxPP() {
        return this.dereference().getMaxPp();
    }
}
