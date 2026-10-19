package de.waterdu.atlantis.mod.pixelmon_old.item;

import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.java.ExceptionUtils;

public final class PixelmonItems {

    private PixelmonItems() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static ConfigurableItemStack pokeBall(PokeBall pokeBall) {
        return PixelmonItems.pokeBall(pokeBall, 1);
    }

    public static ConfigurableItemStack pokeBall(PokeBall pokeBall, int count) {
        return ConfigurableItemStack.of(pokeBall.getBallItem(count));
    }
}
