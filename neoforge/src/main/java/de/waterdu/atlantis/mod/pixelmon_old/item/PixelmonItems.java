package de.waterdu.atlantis.mod.pixelmon_old.item;

import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.java.ExceptionUtils;

/**
 * Creates item stacks of Pixelmon items.
 */
public final class PixelmonItems {

    private PixelmonItems() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Creates an item stack of one Poké Ball.
     *
     * @param pokeBall the type of Poké Ball
     * @return a new item stack
     */
    public static ConfigurableItemStack pokeBall(PokeBall pokeBall) {
        return PixelmonItems.pokeBall(pokeBall, 1);
    }

    /**
     * Creates an item stack of Poké Balls.
     *
     * @param pokeBall the type of Poké Ball
     * @param count the number of items in the stack
     * @return a new item stack
     */
    public static ConfigurableItemStack pokeBall(PokeBall pokeBall, int count) {
        return ConfigurableItemStack.of(pokeBall.getBallItem(count));
    }
}
