package de.waterdu.atlantis.ui.internal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class AtlantisSlotInternal extends Slot {

    private final AtlantisInventoryInternal inventory;

    public AtlantisSlotInternal(AtlantisInventoryInternal inventory, int slot, int x, int y) {
        super(inventory, slot, x, y);
        this.inventory = inventory;
    }

    @Override
    public boolean mayPickup(@NonNull Player player) {
        return this.inventory.isDraggable();
    }

    @Override
    public void onTake(@NonNull Player player, @NonNull ItemStack stack) {
        if (this.inventory.isDraggable()) {
            super.onTake(player, stack);
        }
    }
}
