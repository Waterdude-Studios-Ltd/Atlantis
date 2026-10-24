package de.waterdu.atlantis.mod.impl.cobblemon.wrapper.pokemon.pokemon.properties;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokeball.PokeBall;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.UPokeBall;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class CobblemonPokeBall extends UPokeBall<PokeBall> {

    public static final ModObjectKey TYPE = new ModObjectKey(Cobblemon.MODID, "poke_ball");

    public CobblemonPokeBall(PokeBall ball) {
        super(TYPE, ball);
    }

    @Override
    public String name() {
        return this.dereference().getName().getPath();
    }

    @Override
    public Component displayName() {
        return this.item().getHoverName();
    }

    @Override
    public ItemStack item() {
        return this.dereference().stack(1);
    }
}
