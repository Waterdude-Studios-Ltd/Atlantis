package de.waterdu.atlantis.ui.internal;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.PageEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.ui.api.Button;
import de.waterdu.atlantis.ui.api.ButtonCollector;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderProxy;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Holds the page and the buttons of an open menu. Unless the page is draggable, the slots show the buttons, and a
 * change to a slot opens the page again instead.
 */
public class AtlantisInventoryInternal extends SimpleContainer {

    protected Page page;
    protected Buttons buttons;

    protected final ButtonCollector collector = ButtonCollector.of(button ->
            this.buttons.setButton(button.index(), button.group(), button.page(), button)
    );

    protected PageOptions options;
    protected boolean draggable;
    protected boolean closed = false;

    protected PlayerReference reference;
    protected AtlantisContainerMenuInternal container;

    /**
     * Creates the inventory with the buttons from {@link Page#addButtons} and the page decorations.
     *
     * @param page the page to show
     * @param player the player that sees the page
     * @param slots the number of container slots
     */
    public AtlantisInventoryInternal(Page page, ServerPlayer player, int slots) {
        this(page, player, slots, null);
    }

    /**
     * @param buttons the buttons to show instead of the buttons from {@link Page#addButtons}. Can be null.
     */
    public AtlantisInventoryInternal(Page page, ServerPlayer player, int slots, Set<Button> buttons) {
        super(slots);
        this.page = page;
        this.reference = PlayerReference.get(player);
        this.options = page.options(this.reference);
        this.buttons = new Buttons(this.options, slots, 1, player);
        if (buttons != null) {
            this.collector.collect(buttons);
        } else {
            page.addButtons(this.reference, this.collector);
        }
        this.draggable = this.options().isDraggable();
        var def = AtlantisData.decorations(page);
        if (def != null) {
            var event = Mods.modLoader().event(UEvent.DECORATE_PAGE).emit(new PageEventPayload.Decorate(this.reference, page, def, def.buttons()));
            if (!event.cancelled()) {
                this.collector.collect(event.decorations());
            }
        }
        this.collector.unlock();
    }

    /**
     * Uses the buttons as they are. Does not call {@link Page#addButtons} and does not add decorations.
     */
    public AtlantisInventoryInternal(Page page, ServerPlayer player, Buttons buttons, int slots) {
        super(slots);
        this.reference = PlayerReference.get(player);
        this.options = page.options(this.reference);
        this.draggable = this.options().isDraggable();
        this.buttons = buttons;
        this.page = page;
        this.collector.unlock();
    }

    /**
     * Creates the inventory. It uses the buttons as they are if they are given, or else collects new buttons from
     * the page.
     *
     * @param page the page to show
     * @param player the player that sees the page
     * @param buttons the buttons to use, or {@code null} to collect new buttons
     * @param slots the number of container slots
     * @return the new inventory
     */
    @ApiStatus.Internal
    public static AtlantisInventoryInternal create(Page page, ServerPlayer player, @Nullable Buttons buttons, int slots) {
        if (buttons != null) {
            return new AtlantisInventoryInternal(page, player, buttons, slots);
        } else {
            return new AtlantisInventoryInternal(page, player, slots);
        }
    }

    /**
     * Sets the menu that shows this inventory.
     *
     * @param container the menu
     */
    public void setContainer(AtlantisContainerMenuInternal container) {
        this.container = container;
    }

    /**
     * @return the options of the page
     */
    public PageOptions options() {
        return this.options;
    }

    /**
     * @return the number of chest rows of the page
     */
    public int rows() {
        return this.options().rows();
    }

    /**
     * Runs on the Atlantis thread pool. Calls {@link Page#onUpdate} and the button update tasks, then sends the
     * display changes.
     */
    public void onUpdate(ServerPlayer player, AtlantisContainerMenuInternal container) {
        Atlantis.THREAD_POOL.submit(() -> {
            if (this.collector.isUnlocked()) {
                long tick = player.level().getGameTime();
                this.buttons = this.page.onUpdate(tick, this.reference, this.buttons);
                this.buttons.invokeUpdateTasks(tick);
            }
            container.updateDisplay(this.reference, false);
            container.updateDisplayEnqueued(this.reference);
        });
    }

    /**
     * Gets the number of container slots. A form factor with rows has 9 slots per row.
     *
     * @return the number of container slots
     */
    @Override
    public int getContainerSize() {
        PageOptions.FormFactor ff = this.options().formFactor();
        return ff.hasRows() ? this.options().rows() * 9 : ff.slots();
    }

    /**
     * Checks if the inventory has no items.
     *
     * @return the vanilla result if the page is draggable, otherwise {@code false}
     */
    @Override
    public boolean isEmpty() {
        if (this.draggable) {
            return super.isEmpty();
        }
        return false;
    }

    /**
     * Gets the item in the slot. Unless the page is draggable, it gets the display item of the button there.
     *
     * @param index the index of the slot
     * @return the item, or {@link ItemStack#EMPTY} if there is no button
     */
    @Override
    public @NonNull ItemStack getItem(int index) {
        if (this.draggable) {
            return super.getItem(index);
        }
        var button = this.buttons.button(index);
        return button == null ? ItemStack.EMPTY : button.display(this.reference);
    }

    /**
     * Removes items from the slot if the page is draggable. Otherwise, it opens the page again with the same buttons.
     *
     * @param index the index of the slot
     * @param count the number of items to remove
     * @return the removed items, or {@link ItemStack#EMPTY} if the page is not draggable
     */
    @Override
    public @NonNull ItemStack removeItem(int index, int count) {
        if (this.draggable) {
            return super.removeItem(index, count);
        }
        AtlantisUI.open(this.reference, this.page, this.buttons);
        return ItemStack.EMPTY;
    }

    /**
     * Removes the stack from the slot if the page is draggable. Otherwise, it opens the page again with the same
     * buttons.
     *
     * @param index the index of the slot
     * @return the removed stack, or {@link ItemStack#EMPTY} if the page is not draggable
     */
    @Override
    public @NonNull ItemStack removeItemNoUpdate(int index) {
        if (this.draggable) {
            return super.removeItemNoUpdate(index);
        }
        AtlantisUI.open(this.reference, this.page, this.buttons);
        return ItemStack.EMPTY;
    }

    /**
     * Sets the item in the slot if the page is draggable. Otherwise, it opens the page again with the same buttons.
     *
     * @param index the index of the slot
     * @param stack the item to set
     */
    @Override
    public void setItem(int index, @NonNull ItemStack stack) {
        if (this.draggable) {
            super.setItem(index, stack);
            return;
        }
        AtlantisUI.open(this.reference, this.page, this.buttons);
    }

    /**
     * Gets the maximum stack size of a slot.
     *
     * @return the vanilla value if the page is draggable, otherwise {@link Integer#MAX_VALUE}
     */
    @Override
    public int getMaxStackSize() {
        if (this.draggable) {
            return super.getMaxStackSize();
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Marks the inventory as changed if the page is draggable. If the menu has no display update marked, it also
     * calls {@link Page#onChanged}.
     */
    @Override
    public void setChanged() {
        if (this.draggable) {
            super.setChanged();
            if (!this.container.enqueueUpdate) {
                this.page.onChanged(this.reference, this, false);
            }
        }
    }

    /**
     * Checks if the player can still use the inventory.
     *
     * @param player the player
     * @return the vanilla result if the page is draggable, otherwise {@code false}
     */
    @Override
    public boolean stillValid(@NonNull Player player) {
        if (this.draggable) {
            return super.stillValid(player);
        }
        return false;
    }

    /**
     * Runs the vanilla open logic, but only if the page is draggable.
     *
     * @param player the player that opens the inventory
     */
    @Override
    public void startOpen(@NonNull Player player) {
        if (this.draggable) {
            super.startOpen(player);
        }
    }

    /**
     * Calls {@link Page#onChanged} for the last time and runs the vanilla close logic, but only if the page is
     * draggable.
     *
     * @param player the player that closes the inventory
     */
    @Override
    public void stopOpen(@NonNull Player player) {
        if (this.draggable) {
            this.page.onChanged(this.reference, this, true);
            super.stopOpen(player);
        }
    }

    /**
     * Checks if the item can go in the slot.
     *
     * @param index the index of the slot
     * @param stack the item
     * @return {@code true} only if the page is draggable
     */
    @Override
    public boolean canPlaceItem(int index, @NonNull ItemStack stack) {
        return this.draggable;
    }

    /**
     * Removes all items, but only if the page is draggable.
     */
    @Override
    public void clearContent() {
        if (this.draggable) {
            super.clearContent();
        }
    }

    /**
     * Wraps the ID into the range of existing button groups.
     *
     * @param groupID the ID of the group
     * @return the wrapped ID
     */
    public int resolveGroup(int groupID) {
        return this.buttons.resolveGroup(groupID);
    }

    /**
     * Gets the button group with the ID. The ID wraps into the range of existing groups.
     *
     * @param groupID the ID of the group
     * @return the group
     */
    public Buttons.ButtonPageGroup group(int groupID) {
        return this.buttons.group(groupID);
    }

    /**
     * @return the title, without placeholders replaced. See {@link #displayName()}.
     */
    public Component name() {
        return this.options().title().compile();
    }

    /**
     * @return the title. Placeholders are replaced if placeholders in pages are enabled.
     */
    public MutableComponent displayName() {
        var title = this.options().title();
        if (Settings.get().isUsePlaceholdersInPages()) {
            title = AtlantisPlaceholderProxy.replace(this.reference.entityDirect(), title);
        }
        return title.compile();
    }

    /**
     * @return the page that the inventory shows
     */
    public Page page() {
        return this.page;
    }

    /**
     * Sets the page that the inventory shows.
     *
     * @param page the page
     */
    public void setPage(Page page) {
        this.page = page;
    }

    /**
     * @return the player that sees the page
     */
    public PlayerReference player() {
        return this.reference;
    }

    /**
     * Sets the player that sees the page.
     *
     * @param reference the player
     */
    public void setPlayer(PlayerReference reference) {
        this.reference = reference;
    }

    /**
     * @return the buttons of the page
     */
    public Buttons buttons() {
        return this.buttons;
    }

    /**
     * Sets the buttons of the page.
     *
     * @param buttons the buttons
     */
    public void setButtons(Buttons buttons) {
        this.buttons = buttons;
    }

    /**
     * @return {@code true} if players can move items in and out of the page
     */
    public boolean isDraggable() {
        return this.draggable;
    }

    /**
     * Sets if players can move items in and out of the page.
     *
     * @param draggable {@code true} to let players move items
     */
    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    /**
     * @return {@code true} if {@link Page#afterClose} already ran
     */
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Sets if the page is closed.
     *
     * @param closed {@code true} if {@link Page#afterClose} already ran
     */
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    /**
     * Does nothing. It exists for Arclight.
     */
    public void setMaxStackSize(int size) {
        // Arclight hack
    }
}