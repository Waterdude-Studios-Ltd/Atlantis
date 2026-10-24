package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.moves;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties.CobblemonElementalType;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.moves.UMoveTemplate;
import net.minecraft.network.chat.Component;

public class CobblemonMoveTemplate extends UMoveTemplate<MoveTemplate> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "move_template");

    public CobblemonMoveTemplate(MoveTemplate move) {
        super(TYPE, move);
    }

    @Override
    public String name() {
        return this.dereference().getName();
    }

    @Override
    public Component displayName() {
        return this.dereference().getDisplayName();
    }

    @Override
    public CobblemonElementalType elementalType() {
        return new CobblemonElementalType(this.dereference().getElementalType());
    }

    @Override
    public CobblemonMoveCategory category() {
        return new CobblemonMoveCategory(this.dereference().getDamageCategory());
    }

    @Override
    public int power() {
        return (int) this.dereference().getPower();
    }

    @Override
    public int accuracy() {
        return (int) this.dereference().getAccuracy();
    }

    @Override
    public int basePP() {
        return this.dereference().getPp();
    }

    @Override
    public int maxPP() {
        return this.dereference().getMaxPp();
    }
}
