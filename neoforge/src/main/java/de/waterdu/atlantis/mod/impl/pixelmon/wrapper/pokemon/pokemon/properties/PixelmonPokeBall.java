package de.waterdu.atlantis.mod.impl.pixelmon.wrapper.pokemon.pokemon.properties;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class PixelmonPokeBall extends UPokeBall<PokeBall> {

    public static final ModObjectKey TYPE = new ModObjectKey(Pixelmon.MODID, "poke_ball");

    public PixelmonPokeBall(PokeBall ball) {
        super(TYPE, ball);
    }

    @Override
    public String name() {
        return this.dereference().getName();
    }

    @Override
    public Component displayName() {
        return this.dereference().getTranslatedName();
    }

    @Override
    public ItemStack item() {
        return this.dereference().getBallItem();
    }
}
