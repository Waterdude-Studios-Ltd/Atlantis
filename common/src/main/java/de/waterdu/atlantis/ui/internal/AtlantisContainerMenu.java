package de.waterdu.atlantis.ui.internal;

import de.waterdu.atlantis.util.entity.PlayerReference;

public interface AtlantisContainerMenu {

    AtlantisInventoryInternal getAquaUIInventory();

    void updateDisplay(PlayerReference player, boolean forceAll);
}
