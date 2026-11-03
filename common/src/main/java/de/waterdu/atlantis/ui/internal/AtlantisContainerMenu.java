package de.waterdu.atlantis.ui.internal;

import de.waterdu.atlantis.util.entity.PlayerReference;

/**
 * Marks a container menu that shows an Atlantis page.
 */
public interface AtlantisContainerMenu {

    /**
     * @return the inventory that holds the page and its buttons
     */
    AtlantisInventoryInternal atlantisInventory();

    /**
     * Marks the display for an update. The next page update sends it.
     *
     * @param player not used
     * @param forceAll true sends the whole menu again, instead of only the changed buttons
     */
    void updateDisplay(PlayerReference player, boolean forceAll);
}
