package de.waterdu.atlantis.ui.internal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

/**
 * Shows a button of an Atlantis page. Players can only take items from it if the page is draggable.
 */
public class AtlantisSlotInternal extends Slot {

    private final AtlantisInventoryInternal inventory;

    /**
     * Creates a slot in the page inventory.
     *
     * @param inventory the page inventory
     * @param slot the index of the slot in the inventory
     * @param x the x position of the slot on the screen
     * @param y the y position of the slot on the screen
     */
    public AtlantisSlotInternal(AtlantisInventoryInternal inventory, int slot, int x, int y) {
        super(inventory, slot, x, y);
        this.inventory = inventory;
    }

    /**
     * Checks if the player can pick up the item in this slot.
     *
     * @param player the player
     * @return {@code true} only if the page is draggable
     */
    @Override
    public boolean mayPickup(@NonNull Player player) {
        return this.inventory.isDraggable();
    }

    /**
     * Runs the vanilla take logic only if the page is draggable.
     *
     * @param player the player that takes the item
     * @param stack the item stack that the player takes
     */
    @Override
    public void onTake(@NonNull Player player, @NonNull ItemStack stack) {
        if (this.inventory.isDraggable()) {
            super.onTake(player, stack);
        }
    }
}
