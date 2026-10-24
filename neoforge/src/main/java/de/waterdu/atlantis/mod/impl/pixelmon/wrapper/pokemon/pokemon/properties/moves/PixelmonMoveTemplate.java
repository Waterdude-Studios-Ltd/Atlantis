package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.moves;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties.PixelmonElementalType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveTemplate;
import net.minecraft.network.chat.Component;

public class PixelmonMoveTemplate extends UMoveTemplate<ImmutableAttack> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "move_template");

    public PixelmonMoveTemplate(ImmutableAttack move) {
        super(TYPE, move);
    }

    @Override
    public String name() {
        return this.dereference().getAttackName();
    }

    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    @Override
    public PixelmonElementalType elementalType() {
        return new PixelmonElementalType(this.dereference().getAttackType());
    }

    @Override
    public PixelmonMoveCategory category() {
        return new PixelmonMoveCategory(this.dereference().getAttackCategory());
    }

    @Override
    public int power() {
        return this.dereference().getBasePower();
    }

    @Override
    public int accuracy() {
        return this.dereference().getAccuracy();
    }

    @Override
    public int basePP() {
        return this.dereference().getPPBase();
    }

    @Override
    public int maxPP() {
        return this.dereference().getPPMax();
    }
}
