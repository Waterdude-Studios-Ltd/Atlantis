package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Wraps a Pixelmon Poké Ball.
 */
public class PixelmonPokeBall extends UPokeBall<PokeBall> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "poke_ball");

    /**
     * Creates a wrapper for a Pixelmon Poké Ball.
     *
     * @param ball the Poké Ball to wrap
     */
    public PixelmonPokeBall(PokeBall ball) {
        super(TYPE, ball);
    }

    /**
     * @return the Pixelmon name of the Poké Ball
     */
    @Override
    public String name() {
        return this.dereference().getName();
    }

    /**
     * @return the translated name of the Poké Ball
     */
    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    /**
     * @return the item of the Poké Ball
     */
    @Override
    public ItemStack item() {
        return this.dereference().getBallItem();
    }
}
