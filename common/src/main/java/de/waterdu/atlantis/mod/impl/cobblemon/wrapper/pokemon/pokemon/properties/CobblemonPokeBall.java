package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokeball.PokeBall;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Wraps a Cobblemon Poké Ball.
 */
public class CobblemonPokeBall extends UPokeBall<PokeBall> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "poke_ball");

    /**
     * Creates a wrapper for a Cobblemon Poké Ball.
     *
     * @param ball the Poké Ball to wrap
     */
    public CobblemonPokeBall(PokeBall ball) {
        super(TYPE, ball);
    }

    /**
     * @return the path of the resource location of the Poké Ball
     */
    @Override
    public String name() {
        return this.dereference().getName().getPath();
    }

    /**
     * @return the hover name of the item of the Poké Ball
     */
    @Override
    public Component displayName() {
        return this.item().getHoverName();
    }

    /**
     * Creates a new stack of one Poké Ball item.
     *
     * @return the item stack
     */
    @Override
    public ItemStack item() {
        return this.dereference().stack(1);
    }
}
