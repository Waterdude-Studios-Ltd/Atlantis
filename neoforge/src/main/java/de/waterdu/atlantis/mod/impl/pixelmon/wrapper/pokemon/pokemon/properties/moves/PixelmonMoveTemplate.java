package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonElementalType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveTemplate;
import net.minecraft.network.chat.Component;

/**
 * Wraps a Pixelmon {@link ImmutableAttack}, which holds the base data of a move.
 */
public class PixelmonMoveTemplate extends UMoveTemplate<ImmutableAttack> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "move_template");

    /**
     * Creates a wrapper for the Pixelmon move template.
     *
     * @param move the Pixelmon move template
     */
    public PixelmonMoveTemplate(ImmutableAttack move) {
        super(TYPE, move);
    }

    /**
     * @return the Pixelmon attack name of the move
     */
    @Override
    public String name() {
        return this.dereference().getAttackName();
    }

    /**
     * @return the translated name of the move
     */
    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    /**
     * @return a wrapper for the elemental type of the move
     */
    @Override
    public PixelmonElementalType elementalType() {
        return new PixelmonElementalType(this.dereference().getAttackType());
    }

    /**
     * @return a wrapper for the category of the move
     */
    @Override
    public PixelmonMoveCategory category() {
        return new PixelmonMoveCategory(this.dereference().getAttackCategory());
    }

    /**
     * @return the base power of the move
     */
    @Override
    public int power() {
        return this.dereference().getBasePower();
    }

    /**
     * @return the accuracy of the move
     */
    @Override
    public int accuracy() {
        return this.dereference().getAccuracy();
    }

    /**
     * @return the base PP of the move
     */
    @Override
    public int basePP() {
        return this.dereference().getPPBase();
    }

    /**
     * @return the PP of the move with all PP Ups applied
     */
    @Override
    public int maxPP() {
        return this.dereference().getPPMax();
    }
}
