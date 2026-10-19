package de.waterdu.atlantis.ext.player;

import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface PlayerExt {

    default PlayerReference reference() {
        return null;
    }

    default Optional<PlayerReference> safeReference() {
        return Optional.ofNullable(this.reference());
    }

    default void openPage(Page page) {

    }

    default ItemStack headItem() {
        return null;
    }

    default int getContainerCounter() {
        return 0;
    }

    default void setContainerCounter(int counter) {

    }

    default void initMenu(AbstractContainerMenu menu) {

    }
}
