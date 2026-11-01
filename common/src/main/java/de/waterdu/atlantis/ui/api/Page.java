package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.ui.internal.AtlantisInventoryInternal;
import de.waterdu.atlantis.ui.internal.Buttons;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface for creating {@link AtlantisUI} pages.
 *
 * @author Sam Plummer sam@waterdu.de
 * @version 1.0
 * @since 02/03/2019
 */
public interface Page {

    Page CLOSURE_PAGE = new Page() {
        @Override
        public boolean closureMarker() {
            return true;
        }
    };

    /**
     * Returns the {@link PageOptions} object for all pages.
     *
     * @param player The user that sees this page.
     * @return The options to apply to this page.
     * @see PageOptions
     */
    default PageOptions options(PlayerReference player) {
        return PageOptions.defaults();
    }

    /**
     * Adds the buttons that the page shows.
     * Buttons are instances of {@link Button}.
     *
     * @param player The user that sees this page.
     * @param collector The button collector that receives the buttons.
     * @see                         Button
     */
    default void addButtons(PlayerReference player, final ButtonCollector collector) {}

    /**
     * Called when the user presses a button on the page.
     *
     * @param player The user that sees this page.
     * @param index The index of the pressed button.
     * @param click The {@link Click} representing the user's action.
     *
     * @deprecated Use functional click handlers instead. See the {@link Button.Builder} methods.
     */
    @Deprecated
    default void onButtonClick(PlayerReference player, int index, Click click) {}

    /**
     * Called each tick.
     *
     * @param tick The tick this call represents.
     * @param player The user that sees this page.
     * @param buttons The buttons object for this page.
     * @return The buttons that this page now shows.
     * @see Buttons
     */
    default Buttons onUpdate(long tick, PlayerReference player, Buttons buttons) {
        return buttons;
    }

    /**
     * Called before the page closes.
     *
     * @param player The user that sees this page.
     * @return Whether to continue to close the page.
     */
    default boolean onClose(PlayerReference player) {
        return true;
    }

    /**
     * Called after the page closes.
     *
     * @param player The user that sees this page.
     */
    default void afterClose(PlayerReference player) {}

    /**
     * Called when a draggable page changes.
     *
     * @param player The user that sees this page.
     * @param inventory The inventory of this page.
     * @param closed Whether the page closure caused this change.
     */
    default void onChanged(PlayerReference player, AtlantisInventoryInternal inventory, boolean closed) {}

    /**
     * Opens the page for a player.
     *
     * @param player The user to show this page to.
     */
    default void open(PlayerReference player) {
        AtlantisUI.open(player, this);
    }

    /**
     * Opens the page for a player.
     *
     * @param player The user to show this page to.
     */
    default void open(Player player) {
        AtlantisUI.open(player, this);
    }

    /**
     * Marker for whether to use this page for page closure, because of how {@link ConcurrentHashMap} is
     * implemented.
     * If you do not know what this is for, it is best not to change it.
     *
     * @return Whether this page is a closure marker.
     */
    @ApiStatus.Internal
    default boolean closureMarker() {
        return false;
    }
}
