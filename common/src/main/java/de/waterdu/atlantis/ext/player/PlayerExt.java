package de.waterdu.atlantis.ext.player;

import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Adds player reference, UI and container helpers to {@link net.minecraft.world.entity.player.Player}. The interface
 * is injected into the player class.
 */
public interface PlayerExt {

    /**
     * @return the reference for this player. null when this is not a server player or the client player.
     */
    default PlayerReference reference() {
        return null;
    }

    /**
     * @return the reference for this player, or an empty optional when {@link #reference()} is {@code null}
     */
    default Optional<PlayerReference> safeReference() {
        return Optional.ofNullable(this.reference());
    }

    /**
     * Opens the page for this player. Does nothing when this is not a server player or the client player.
     *
     * @param page the page to open
     */
    default void openPage(Page page) {

    }

    /**
     * @return the player's head from the player data, or a plain player head when the data is not loaded yet. null when
     *     this is not a server player or the client player.
     */
    default ItemStack headItem() {
        return null;
    }

    /**
     * @return the ID counter of the open container of the server player. {@code 0} for other players.
     */
    default int getContainerCounter() {
        return 0;
    }

    /**
     * Sets the ID counter of the open container of the server player. Does nothing for other players.
     *
     * @param counter the new container counter
     */
    default void setContainerCounter(int counter) {

    }

    /**
     * Initializes the menu for the server player, which adds the slot listener and sends the contents. Does nothing
     * for other players.
     *
     * @param menu the menu to initialize
     */
    default void initMenu(AbstractContainerMenu menu) {

    }
}
