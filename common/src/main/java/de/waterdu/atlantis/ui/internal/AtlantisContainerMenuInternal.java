package de.waterdu.atlantis.ui.internal;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.ext.player.PlayerExt;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.PageEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.ui.api.Button;
import de.waterdu.atlantis.ui.api.Click;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shows an Atlantis page as a container menu. Clicks go to the page buttons. Unless the page is draggable, players
 * cannot move items, and the player inventory slots also show page buttons.
 */
public class AtlantisContainerMenuInternal extends AbstractContainerMenu implements AtlantisContainerMenu {

    /**
     * The slot ID that vanilla sends for a click outside the screen.
     */
    public static final int OUTSIDE_SCREEN_INDEX = -999;
    private static final Button OUTSIDE_SCREEN_BUTTON = Button.emptyButton(OUTSIDE_SCREEN_INDEX);

    protected List<ItemStack> inventoryItemStacks = NonNullList.create();
    protected List<Slot> inventorySlots = new ArrayList<>();

    protected final AtlantisInventoryInternal inventory;
    protected Click previousClick = null;
    protected boolean enqueueUpdate = false;
    protected final AtomicInteger updates = new AtomicInteger(0);
    protected boolean changingPage = false;
    protected boolean forceAll = false;

    /**
     * Creates the menu and adds its slots. A draggable page uses the real player inventory slots. Other pages use
     * page slots in their place.
     *
     * @param inventory the page inventory
     * @param player the player that opens the menu
     */
    public AtlantisContainerMenuInternal(AtlantisInventoryInternal inventory, Player player) {
        super(inventory.options().formFactor().getType(inventory.options()), 1);

        var playerInventory = player.getInventory();
        
        this.inventory = inventory;
        inventory.setContainer(this);
        inventory.startOpen(player);
        var draggable = inventory.isDraggable();

        var slot = 0;
        if (inventory.options().formFactor().hasRows()) {
            var numRows = inventory.getContainerSize() / 9;
            var dy = (numRows - 4) * 18;

            for (var y = 0; y < numRows; ++y) {
                for (var x = 0; x < 9; ++x) {
                    this.addSlot(new AtlantisSlotInternal(inventory, draggable ? x + y * 9 : slot++, 8 + x * 18, 18 + y * 18));
                }
            }

            for (var y = 0; y < 3; ++y) {
                for (var x = 0; x < 9; ++x) {
                    if (draggable) {
                        this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 103 + y * 18 + dy));
                    } else {
                        this.addSlot(new AtlantisSlotInternal(inventory, slot++, 8 + x * 18, 103 + y * 18 + dy));
                    }
                }
            }

            for (var x = 0; x < 9; ++x) {
                if (draggable) {
                    this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 161 + dy));
                } else {
                    this.addSlot(new AtlantisSlotInternal(inventory, slot++, 8 + x * 18, 161 + dy));
                }
            }
        } else {
            for (var j = 0; j < this.inventory.getContainerSize(); ++j) {
                this.addSlot(new AtlantisSlotInternal(this.inventory, draggable ? j : slot++, 0, 0));
            }

            for (var y = 0; y < 3; ++y) {
                for (var x = 0; x < 9; ++x) {
                    if (draggable) {
                        this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, y * 18 + 51));
                    } else {
                        this.addSlot(new AtlantisSlotInternal(inventory, slot++, 8 + x * 18, y * 18 + 51));
                    }
                }
            }

            for (var x = 0; x < 9; ++x) {
                if (draggable) {
                    this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 109));
                } else {
                    this.addSlot(new AtlantisSlotInternal(inventory, slot++, 8 + x * 18, 109));
                }
            }
        }
    }

    /**
     * Adds the slot to the menu and to {@link #inventorySlots}. For a page that is not draggable, it also sets the
     * slot index.
     *
     * @param slotIn the slot to add
     * @return the added slot
     */
    @Override
    protected @NonNull Slot addSlot(@NonNull Slot slotIn) {
        if (this.inventory.isDraggable()) {
            var slot = super.addSlot(slotIn);
            this.inventorySlots.add(slot);
            this.inventoryItemStacks.add(ItemStack.EMPTY);
            return slot;
        }
        slotIn.index = this.inventorySlots.size();
        this.inventorySlots.add(slotIn);
        this.inventoryItemStacks.add(ItemStack.EMPTY);
        super.addSlot(slotIn);
        return slotIn;
    }

    /**
     * @return the inventory that holds the page and its buttons
     */
    @Override
    public AtlantisInventoryInternal atlantisInventory() {
        return this.inventory;
    }

    /**
     * Checks if the player can still use the menu.
     *
     * @param playerIn the player
     * @return always {@code true}
     */
    @Override
    public boolean stillValid(@NonNull Player playerIn) {
        return true;
    }

    /**
     * Moves the stack between the page and the player inventory if the page is draggable. Otherwise, it resets the
     * display and moves nothing.
     *
     * @param player the player that moves the stack
     * @param index the index of the clicked slot
     * @return a copy of the moved stack, or {@link ItemStack#EMPTY} if nothing moves
     */
    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        if (this.inventory.isDraggable()) {
            var stackCopy = ItemStack.EMPTY;
            var slot = this.slots.get(index);
            if (slot.hasItem()) {
                var stack = slot.getItem();
                stackCopy = stack.copy();
                if (index < this.inventory.getContainerSize()) {
                    if (!this.moveItemStackTo(stack, this.inventory.getContainerSize(), this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, 0, this.inventory.getContainerSize(), false)) {
                    return ItemStack.EMPTY;
                }
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }
            return stackCopy;
        }
        this.resetDisplay();
        return ItemStack.EMPTY;
    }

    /**
     * Makes a {@link Click} and sends it to the button in the slot. A draggable page also runs the vanilla click
     * logic. Other pages reset the client state instead.
     *
     * @param slot the index of the clicked slot, or {@link #OUTSIDE_SCREEN_INDEX}
     * @param dragType the mouse button or drag state that vanilla sends
     * @param clickTypeIn the type of click
     * @param player the player that clicks
     */
    @Override
    public void clicked(int slot, int dragType, @NonNull ClickType clickTypeIn, @NonNull Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var button = this.resolveButton(slot);

            var click = Click.make(
                    this.inventory.player(),
                    this.inventory.buttons(),
                    slot,
                    button.page(),
                    button.group(),
                    Click.Button.get(dragType),
                    Click.Style.get(clickTypeIn),
                    this.previousClick
            );
            this.previousClick = click;

            if (this.inventory.isDraggable()) {
                this.doButtonClick(slot, button, click);
                super.clicked(slot, dragType, clickTypeIn, player);
                return;
            }

            this.resetClientState(serverPlayer);
            this.doButtonClick(slot, button, click);
        }
    }

    /**
     * Gets the button in the slot.
     *
     * @param slot the index of the slot, or {@link #OUTSIDE_SCREEN_INDEX}
     * @return the button, or an empty button for a click outside the screen
     */
    private Button resolveButton(int slot) {
        return slot == OUTSIDE_SCREEN_INDEX ? OUTSIDE_SCREEN_BUTTON : this.inventory.buttons().button(slot);
    }

    /**
     * Clears the carried item on the client and marks the whole display for an update.
     *
     * @param serverPlayer the player to reset
     */
    private void resetClientState(ServerPlayer serverPlayer) {
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(-1, 1, 0, ItemStack.EMPTY));
        this.resetDisplay();
    }

    /**
     * Handles the click on {@link Atlantis#THREAD_POOL}. If the button has presses left and the player is not in
     * {@link AtlantisUI#LOCKED}, it emits {@link UEvent#PRESS_PAGE}. If no listener cancels the event, the page and
     * the button receive the click. Then the player is locked until the next display update.
     *
     * @param slot the index of the clicked slot
     * @param button the clicked button
     * @param click the click
     */
    private void doButtonClick(final int slot, final Button button, final Click click) {
        Atlantis.THREAD_POOL.submit(() -> {
            if (button.presses() == Integer.MIN_VALUE || button.presses() > 0) {
                if (button.presses() > 0) {
                    button.changePresses(-1);
                }
                if (!AtlantisUI.LOCKED.contains(this.inventory.player().uuid())) {
                    if (!Mods.modLoader().event(UEvent.PRESS_PAGE).emit(new PageEventPayload.Press(this.inventory.player(), this.inventory.page(), button, click)).cancelled()) {
                        Concurrency.sync(() -> this.inventory.page().onButtonClick(this.inventory.player(), slot, click));
                        button.performOnClick(button.isAsync(), this.inventory.page, click);
                    }
                    AtlantisUI.LOCKED.add(this.inventory.player().uuid());
                }
            }
        });
    }

    /**
     * Moves the stack into the slot range with the vanilla logic, but only if the page is draggable.
     *
     * @param stack the stack to move
     * @param start the first slot index of the range
     * @param end the slot index after the end of the range
     * @param backwards {@code true} to fill the range from the end
     * @return {@code true} if a part of the stack moved. Always {@code false} if the page is not draggable.
     */
    @Override
    protected boolean moveItemStackTo(@NonNull ItemStack stack, int start, int end, boolean backwards) {
        if (this.inventory.isDraggable()) {
            return super.moveItemStackTo(stack, start, end, backwards);
        }
        return false;
    }

    /**
     * Checks if a double click can collect items from the slot.
     *
     * @param stack the stack that the player collects
     * @param slotIn the slot to collect from
     * @return the vanilla result if the page is draggable, otherwise {@code false}
     */
    @Override
    public boolean canTakeItemForPickAll(@NonNull ItemStack stack, @NonNull Slot slotIn) {
        if (this.inventory.isDraggable()) {
            return super.canTakeItemForPickAll(stack, slotIn);
        }
        return false;
    }

    /**
     * Closes the menu and puts the player back in their inventory menu. If the page refuses to close, or a listener
     * cancels {@link UEvent#DESTRUCT_PAGE}, the page opens again. A page change does not count as a close.
     *
     * @param playerIn the player that closes the menu
     */
    @Override
    public void removed(@NonNull Player playerIn) {
        if (playerIn instanceof ServerPlayer player) {
            var reference = PlayerReference.get(player);
            super.removed(playerIn);
            this.inventory.stopOpen(playerIn);
            player.connection.send(new ClientboundContainerClosePacket(player.containerMenu.containerId));
            player.setContainerCounter(0);
            player.containerMenu = player.inventoryMenu;
            player.containerMenu.broadcastChanges();
            ((PlayerExt) player).initMenu(player.containerMenu);

            if (this.inventory.isDraggable()) {
                if (!this.inventory.isClosed()) {
                    this.inventory.page().afterClose(reference);
                    this.inventory.setClosed(true);
                }
                return;
            }

            if (this.changingPage) {
                this.changingPage = false;
            } else {
                if (!this.inventory.page().onClose(reference) || Mods.modLoader().event(UEvent.DESTRUCT_PAGE).emit(new PageEventPayload.Destruct(reference, this.inventory.page())).cancelled()) {
                    AtlantisUI.open(player, this.inventory.page(), this.inventory.buttons(), true);
                } else {
                    if (!this.inventory.isClosed()) {
                        this.inventory.page().afterClose(reference);
                        this.inventory.setClosed(true);
                    }
                }
            }
        }
    }

    /**
     * Sets the item in the slot, but only if the page is draggable.
     *
     * @param slotId the index of the slot
     * @param stateId the state ID of the menu
     * @param stack the item to set
     */
    @Override
    public void setItem(int slotId, int stateId, @NonNull ItemStack stack) {
        if (this.inventory.isDraggable()) {
            super.setItem(slotId, stateId, stack);
        }
    }

    /**
     * Checks if a drag can put items in the slot.
     *
     * @param slot the slot
     * @return the vanilla result if the page is draggable, otherwise {@code false}
     */
    @Override
    public boolean canDragTo(@NonNull Slot slot) {
        if (this.inventory.isDraggable()) {
            return super.canDragTo(slot);
        }
        return false;
    }

    /**
     * Marks the whole display for an update.
     */
    public void resetDisplay() {
        this.updateDisplay(true);
    }

    /**
     * Marks the display of the page player for an update.
     *
     * @param forceAll {@code true} sends the whole menu again, instead of only the changed buttons
     */
    public void updateDisplay(boolean forceAll) {
        this.updateDisplay(this.inventory.player(), forceAll);
    }

    /**
     * Marks the display for an update. The next page update sends it.
     *
     * @param player not used
     * @param forceAll {@code true} sends the whole menu again, instead of only the changed buttons
     */
    public void updateDisplay(PlayerReference player, boolean forceAll) {
        this.enqueueUpdate = true;
        this.forceAll = this.forceAll || forceAll;
    }

    /**
     * Sends the update that {@link #updateDisplay(PlayerReference, boolean)} marked, and removes the player from
     * {@link AtlantisUI#LOCKED}. A draggable page only receives its buttons on the first call.
     */
    public void updateDisplayEnqueued(PlayerReference player) {
        var entity = player.entityDirect();
        if (entity == null) {
            return;
        }

        if (this.inventory.isDraggable()) {
            if (this.updates.getAndIncrement() == 0) {
                for (var i = 0; i < this.inventory.buttons().size(true); i++) {
                    var button = this.inventory.buttons().button(i);
                    var display = button.isEmpty() ? ItemStack.EMPTY : button.display(player);
                    this.inventory.buttons().setButton(i, button.group(), button.page(), button);
                    this.setItem(i, this.getStateId(), display);
                }
                this.forceAll = false;
            }
            this.enqueueUpdate = false;
            AtlantisUI.LOCKED.remove(player.uuid());
        } else {
            Atlantis.THREAD_POOL.submit(() -> {
                if (this.enqueueUpdate) {
                    for (var i = 0; i < this.inventory.buttons().size(true); i++) {
                        var button = this.inventory.buttons().button(i);
                        if (this.forceAll || (button != null && button.isChanged())) {
                            button.markClean();
                            if (!this.forceAll) {
                                entity.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.getStateId(), i, button.isEmpty() ? ItemStack.EMPTY : button.display(player)));
                            }
                        }
                    }
                    if (this.forceAll) {
                        this.forceAll = false;
                        entity.initMenu(this);
                    }
                    this.enqueueUpdate = false;
                    AtlantisUI.LOCKED.remove(player.uuid());
                }
            });
        }
    }

    /**
     * Gets the slot from {@link #inventorySlots}. An index past the end gets the last slot.
     *
     * @param slotId the index of the slot
     * @return the slot
     */
    @Override
    public @NonNull Slot getSlot(int slotId) {
        return this.inventorySlots.get(Math.min(this.inventorySlots.size() - 1, slotId));
    }

    /**
     * Sends the changed slots to the client, but only if the page is draggable.
     */
    @Override
    public void broadcastChanges() {
        if (this.inventory.isDraggable()) {
            super.broadcastChanges();
        }
    }

    /**
     * Gets the item that the player carries with the cursor.
     *
     * @return the carried item if the page is draggable, otherwise {@link ItemStack#EMPTY}
     */
    @Override
    public @NonNull ItemStack getCarried() {
        if (this.inventory.isDraggable()) {
            return super.getCarried();
        }
        return ItemStack.EMPTY;
    }

    /**
     * @return the item stacks of the menu slots
     */
    public List<ItemStack> getInventoryItemStacks() {
        return this.inventoryItemStacks;
    }

    /**
     * Sets the item stacks of the menu slots.
     *
     * @param inventoryItemStacks the item stacks
     */
    public void setInventoryItemStacks(List<ItemStack> inventoryItemStacks) {
        this.inventoryItemStacks = inventoryItemStacks;
    }

    /**
     * @return the slots of the menu
     */
    public List<Slot> getInventorySlots() {
        return this.inventorySlots;
    }

    /**
     * Sets the slots of the menu.
     *
     * @param inventorySlots the slots
     */
    public void setInventorySlots(List<Slot> inventorySlots) {
        this.inventorySlots = inventorySlots;
    }
}