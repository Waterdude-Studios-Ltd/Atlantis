package de.waterdu.atlantis.ui.internal;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.ui.api.Button;
import de.waterdu.atlantis.ui.api.ButtonUpdateTask;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.collection.ArrayUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the buttons of an open page. Buttons are put in groups, and each group has one or more pages. Only the
 * current page of each group is visible.
 */
public class Buttons {

    private static final Button EMPTY_BUTTON = Button.emptyButton();

    private final PageOptions options;
    private final int initialSize;
    private final int initialGroupCount;
    private final PlayerReference player;

    private final Map<Integer, ButtonPageGroup> groups;
    private final Map<Button, List<ButtonUpdateTask>> updates = new ConcurrentHashMap<>();

    private final ButtonPageGroup baseGroup;
    private final Button[] emptyButtons;

    /**
     * Creates the buttons with at most one group at the start. More groups are made when a button needs them.
     *
     * @param options the options of the page
     * @param size the number of container slots
     * @param groups the number of groups to make at the start, limited to 1
     * @param player the player that sees the page
     */
    public Buttons(PageOptions options, int size, int groups, ServerPlayer player) {
        this.options = options;
        this.initialSize = size;
        this.initialGroupCount = Math.min(1, groups);
        this.player = PlayerReference.get(player);
        this.groups = new Int2ObjectOpenHashMap<>(this.initialGroupCount);
        for (var group = 0; group < this.initialGroupCount; group++) {
            this.groups.put(group, new ButtonPageGroup(group, this.initialSize, this.player, this.options));
        }
        this.baseGroup = this.group(0);
        this.emptyButtons = ArrayUtils.arrayOf(Button.class, this.initialSize + Inventory.INVENTORY_SIZE, Button::emptyButton);
    }

    /**
     * Creates an array of empty buttons, each with its own index.
     *
     * @param size the length of the array
     * @param pageID the page of the buttons
     * @return the new array
     */
    private static Button[] makeEmptyButtonArray(int size, int pageID) {
        var buttons = new Button[size];
        for (var i = 0; i < size; i++) {
            buttons[i] = Button.emptyButton(i, pageID);
        }
        return buttons;
    }

    /**
     * Gets the container buttons of page 0 of group 0.
     *
     * @return the backing array. Changes to it change the page.
     */
    public Button[] containerButtons() {
        return this.containerButtons(0, 0);
    }

    /**
     * @return the backing array. Changes to it change the page.
     */
    public Button[] containerButtons(int groupID, int pageID) {
        return this.groups.get(this.resolveGroup(groupID)).page(pageID).container;
    }

    /**
     * Copies the buttons into the container of page 0 of group 0. Their update tasks are not registered.
     *
     * @param buttons the buttons to copy
     */
    public void setContainerButtons(Button[] buttons) {
        this.setContainerButtons(buttons, 0, 0);
    }

    /**
     * Copies the buttons into the page. Their update tasks are not registered.
     */
    public void setContainerButtons(Button[] buttons, int groupID, int pageID) {
        var groupButtons = this.groups.get(this.resolveGroup(groupID)).page(pageID).container;
        System.arraycopy(buttons, 0, groupButtons, 0, groupButtons.length);
    }

    /**
     * Gets the player inventory buttons of page 0 of group 0.
     *
     * @return the backing array. Changes to it change the page.
     */
    public Button[] inventoryButtons() {
        return this.inventoryButtons(0, 0);
    }

    /**
     * @return the backing array. Changes to it change the page.
     */
    public Button[] inventoryButtons(int groupID, int pageID) {
        return this.groups.get(this.resolveGroup(groupID)).page(pageID).inventory;
    }

    /**
     * Copies the buttons into the player inventory of page 0 of group 0. Their update tasks are not registered.
     *
     * @param buttons the buttons to copy
     */
    public void setInventoryButtons(Button[] buttons) {
        this.setInventoryButtons(buttons, 0, 0);
    }

    /**
     * Copies the buttons into the page. Their update tasks are not registered.
     */
    public void setInventoryButtons(Button[] buttons, int groupID, int pageID) {
        var inventoryButtons = this.groups.get(this.resolveGroup(groupID)).page(pageID).inventory;
        System.arraycopy(buttons, 0, inventoryButtons, 0, inventoryButtons.length);
    }

    /**
     * Runs the consumer for each button on each page of each group.
     *
     * @param counter the counter for the first button
     * @param forEach the consumer to run
     * @return the counter that the last call returned
     */
    public int forEachButton(int counter, ButtonConsumer forEach) {
        for (var group : this.groups.values()) {
            counter = group.forEachButton(counter, forEach);
        }
        return counter;
    }

    /**
     * Runs the consumer for each button on the current page of each group.
     *
     * @param counter the counter for the first button
     * @param forEach the consumer to run
     * @return the counter that the last call returned
     */
    public int forEachVisibleButton(int counter, ButtonConsumer forEach) {
        for (var group : this.groups.values()) {
            counter = group.currentPage().forEachButton(counter, group.group, forEach);
        }
        return counter;
    }

    /**
     * Runs the update tasks of each visible button.
     *
     * @param tick the current game time
     */
    public void invokeUpdateTasks(long tick) {
        var entries = this.updates.entrySet();
        for (var entry : entries) {
            for (var task : entry.getValue()) {
                if (entry.getKey().isVisible(this)) {
                    task.invoke(entry.getKey(), tick);
                }
            }
        }
    }

    /**
     * Sends a slot packet for each visible button to the player. The slot numbers start after the container slots.
     *
     * @param windowID the container ID of the menu
     * @param state the state ID of the menu
     * @param player the player to send the packets to. Nothing is sent if the player is offline.
     */
    public void forceInventoryDisplay(int windowID, int state, PlayerReference player) {
        player.entity().ifPresent(entity -> this.forEachVisibleButton(this.size(false), (counter, button, g, p) -> {
            entity.connection.send(new ClientboundContainerSetSlotPacket(windowID, state, counter++, button.display(player)));
            return counter;
        }));
    }

    /**
     * Returns the first non-empty button at the index on the current page of each group. If no group has one, or
     * the index is out of bounds, returns an empty button.
     */
    public Button button(int index) {
        if (index < 0 || index >= this.size()) {
            AtlantisLogger.debug("Button access out of bounds. This might be a problem with your config, or an Atlantis plugin.");
            return Button.emptyButton();
        }

        var emptyButton = this.emptyButtons[index];
        for (var group : this.groups.values()) {
            var page = group.currentPage();
            var button = page.getButton(index);
            if (button != null && !button.isEmpty()) {
                emptyButton.markDirty();
                return button;
            }
        }
        return emptyButton;
    }

    /**
     * Gets the button at the index on a page of a group. The group and page IDs wrap into the existing range.
     *
     * @param index the index of the button
     * @param groupID the ID of the group
     * @param pageID the ID of the page
     * @return the button, or an empty button if the index is out of bounds
     */
    public Button button(int index, int groupID, int pageID) {
        return this.button(index, this.group(groupID), pageID);
    }

    /**
     * Gets the button at the index on a page of the group.
     *
     * @param index the index of the button
     * @param group the group
     * @param pageID the ID of the page
     * @return the button, or an empty button if the index is out of bounds
     */
    private Button button(int index, ButtonPageGroup group, int pageID) {
        return group.page(pageID).getButton(index);
    }

    /**
     * Sets the button on page 0 of group 0.
     *
     * @param index the index of the button
     * @param button the button to set
     */
    public void setButton(int index, Button button) {
        this.setButton(index, 0, 0, button);
    }

    /**
     * Creates the group and page if they do not exist. A low-priority button does not replace a non-empty button.
     *
     * @param index indexes after the container slots are player inventory slots
     * @param pageID -1 sets the button on all pages that exist
     */
    public void setButton(int index, int groupID, int pageID, Button button) {
        this.updateGroupCount(groupID);
        this.setButton(index, this.group(groupID), pageID, button);
    }

    /**
     * Sets the button on a page of the group, and creates the page if it does not exist. A low-priority button does
     * not replace a non-empty button.
     *
     * @param index the index of the button
     * @param group the group
     * @param pageID the ID of the page. -1 sets the button on all pages that exist.
     * @param button the button to set
     */
    private void setButton(int index, ButtonPageGroup group, int pageID, Button button) {
        if (pageID == -1) {
            for (var i = 0; i < group.pages.size(); i++) {
                this.setButton(index, group, i, button);
            }
            return;
        }

        group.updatePageCount(pageID);
        var page = group.page(pageID);
        if (this.isWithinBounds(index)) {
            if (index < page.container.length) {
                if (!button.isLowPriority() || page.container[index].isEmpty()) {
                    this.setButtonInternal(button, index, page.container);
                }
            } else if (index < page.container.length + page.inventory.length &&
                    (!button.isLowPriority() || page.inventory[index - page.container.length].isEmpty())
            ) {
                this.setButtonInternal(button, index - page.container.length, page.inventory);
            }
        }
    }

    /**
     * Puts the button in the array and registers its update tasks. It removes the update tasks of the button that
     * it replaces.
     *
     * @param button the button to put
     * @param index the index in the array
     * @param array the container or player inventory array of a page
     */
    private void setButtonInternal(Button button, int index, Button[] array) {
        var previous = array[index];
        if (!Objects.equals(button, previous)) {
            this.updates.remove(previous);
        }
        array[index] = button;
        var tasks = array[index].updateTasks();
        if (tasks.isEmpty()) {
            this.updates.remove(button);
        } else {
            this.updates.put(button, tasks);
        }
    }

    /**
     * Creates groups until the group with the ID exists.
     *
     * @param group the ID of the group that must exist
     */
    private void updateGroupCount(int group) {
        if (group >= this.groups.size()) {
            var target = group + 1;
            while (this.groups.size() < target) {
                this.groups.put(this.groups.size(), new ButtonPageGroup(
                        this.groups.size(),
                        this.initialSize,
                        this.player,
                        this.options
                ));
            }
        }
    }

    /**
     * Clears the index on the current page of each group.
     */
    public void clearButton(int index) {
        for (var group : this.groups.values()) {
            this.clearButton(index, group, group.currentPage);
        }
    }

    /**
     * Replaces the button at the index on a page of a group with an empty button.
     *
     * @param index the index of the button
     * @param groupID the ID of the group
     * @param pageID the ID of the page
     */
    public void clearButton(int index, int groupID, int pageID) {
        this.clearButton(index, this.group(groupID), pageID);
    }

    /**
     * Replaces the button at the index on a page of the group with an empty button.
     *
     * @param index the index of the button
     * @param group the group
     * @param pageID the ID of the page
     */
    private void clearButton(int index, ButtonPageGroup group, int pageID) {
        var page = group.page(pageID);
        if (this.isWithinBounds(index)) {
            if (index < page.container.length) {
                page.container[index] = Button.emptyButton(index, pageID, group.group);
            } else if (index < page.container.length + page.inventory.length) {
                page.inventory[index - page.container.length] = Button.emptyButton(index, pageID, group.group);
            }
        }
    }

    /**
     * Copies the current pages, and the update tasks of buttons in the same place, from {@code other}.
     */
    public void persist(Buttons other) {
        for (var group : this.groups.values()) {
            var otherGroup = other.group(group.group);
            if (group.group == otherGroup.group) {
                group.setCurrentPage(otherGroup.currentPage);
            }
            group.forEachButton(0, (c, button, g, p) -> {
                var otherButton = otherGroup.page(p).getButton(g);
                if (button.isSamePlace(otherButton)) {
                    button.copyUpdateTasks(otherButton);
                }
                return c;
            });
        }
    }

    /**
     * Checks if the index is a container or player inventory slot.
     *
     * @param index the index to check
     * @return {@code true} if the index is within the bounds
     */
    public boolean isWithinBounds(int index) {
        return index >= 0 && index < this.size();
    }

    /**
     * @return the number of container and player inventory slots
     */
    public int size() {
        return this.size(true);
    }

    /**
     * @param all true includes the player inventory slots
     */
    public int size(boolean all) {
        var page = this.baseGroup.pageZero;
        return page.container.length + (all ? page.inventory.length : 0);
    }

    /**
     * Gets the display items of the buttons on page 0 of group 0.
     *
     * @param all {@code true} includes the player inventory slots
     * @return the display items. Empty buttons give {@link ItemStack#EMPTY}.
     */
    public NonNullList<ItemStack> asItemStacks(boolean all) {
        return this.asItemStacks(0, 0, all);
    }

    /**
     * Gets the display items of the buttons on a page of a group.
     *
     * @param groupID the ID of the group
     * @param pageID the ID of the page
     * @param all {@code true} includes the player inventory slots
     * @return the display items. Empty buttons give {@link ItemStack#EMPTY}.
     */
    public NonNullList<ItemStack> asItemStacks(int groupID, int pageID, boolean all) {
        var stacks = NonNullList.withSize(this.size(all), ItemStack.EMPTY);
        var group = this.group(groupID);
        for (var i = 0; i < this.size(all); i++) {
            var button = this.button(i, group, pageID);
            if (!button.isEmpty()) {
                stacks.set(i, button.display(this.player));
            }
        }
        return stacks;
    }

    /**
     * Gets the display items of the visible buttons, as {@link #button(int)} finds them.
     *
     * @param all {@code true} includes the player inventory slots
     * @return the display items. Empty buttons give {@link ItemStack#EMPTY}.
     */
    public NonNullList<ItemStack> asVisibleItemStacks(boolean all) {
        var stacks = NonNullList.withSize(this.size(all), ItemStack.EMPTY);
        for (var i = 0; i < this.size(all); i++) {
            var button = this.button(i);
            if (!button.isEmpty()) {
                stacks.set(i, button.display(this.player));
            }
        }
        return stacks;
    }

    /**
     * Wraps the ID into the range of existing groups.
     */
    public int resolveGroup(int groupID) {
        return Math.floorMod(groupID, this.groups.size());
    }

    /**
     * Gets the group with the ID. The ID wraps into the range of existing groups.
     *
     * @param groupID the ID of the group
     * @return the group
     */
    public ButtonPageGroup group(int groupID) {
        return this.groups.get(this.resolveGroup(groupID));
    }

    /**
     * @return group 0
     */
    public ButtonPageGroup baseGroup() {
        return this.baseGroup;
    }

    /**
     * @return the number of groups
     */
    public int groupCount() {
        return this.groups.size();
    }

    /**
     * Holds the pages of one button group, and tracks which page is current.
     */
    public static class ButtonPageGroup {

        private final int group;
        private final List<ButtonPage> pages;

        private final ButtonPage pageZero;

        private int currentPage;

        /**
         * Creates the group with page 0.
         *
         * @param group the ID of the group
         * @param size the number of container slots
         * @param player the player that sees the page
         * @param options the options of the page
         */
        private ButtonPageGroup(int group, int size, PlayerReference player, PageOptions options) {
            this.group = group;
            this.pages = new ArrayList<>();
            this.currentPage = 0;
            this.pageZero = new ButtonPage(this, size, 0, player, options);
            this.pages.add(this.pageZero);
        }

        /**
         * Wraps the page into the range of existing pages.
         */
        public int resolvePage(int page) {
            return Math.floorMod(page, this.pages.size());
        }

        /**
         * @return the current page
         */
        public ButtonPage currentPage() {
            return this.page(this.currentPage);
        }

        /**
         * Gets the page with the ID. The ID wraps into the range of existing pages.
         *
         * @param page the ID of the page
         * @return the page
         */
        public ButtonPage page(int page) {
            return this.pages.get(this.resolvePage(page));
        }

        /**
         * Sets the current page. The ID is stored as it is, and wraps only when the page is read.
         *
         * @param page the ID of the page
         */
        public void setCurrentPage(int page) {
            this.currentPage = page;
        }

        /**
         * @return the ID of the current page, as it was set
         */
        public int currentPageId() {
            return this.currentPage;
        }

        /**
         * @return the number of pages in the group
         */
        public int pageCount() {
            return this.pages.size();
        }

        /**
         * Runs the consumer for each button on each page of the group.
         *
         * @param counter the counter for the first button
         * @param forEach the consumer to run
         * @return the counter that the last call returned
         */
        public int forEachButton(int counter, ButtonConsumer forEach) {
            for (var page : this.pages) {
                counter = page.forEachButton(counter, this.group, forEach);
            }
            return counter;
        }

        /**
         * Creates pages until the page with the ID exists.
         *
         * @param page the ID of the page that must exist
         */
        private void updatePageCount(int page) {
            if (page >= this.pages.size()) {
                int target = page + 1;
                while (this.pages.size() < target) {
                    this.pages.add(new ButtonPage(
                            this,
                            this.pages.size(),
                            this.makeContainerButtonArray(this.pages.size()),
                            this.makeInventoryButtonArray(this.pages.size())
                    ));
                }
            }
        }

        /**
         * Creates the container buttons for a new page from page 0.
         *
         * @param pageID the ID of the new page
         * @return the new array
         */
        private Button[] makeContainerButtonArray(int pageID) {
            return this.makeButtonArray(this.pageZero.container, pageID);
        }

        /**
         * Creates the player inventory buttons for a new page from page 0.
         *
         * @param pageID the ID of the new page
         * @return the new array
         */
        private Button[] makeInventoryButtonArray(int pageID) {
            return this.makeButtonArray(this.pageZero.inventory, pageID);
        }

        /**
         * Creates the buttons for a new page. It keeps the buttons of the base that are on all pages (page -1), and
         * uses empty buttons for the rest.
         *
         * @param base the buttons of page 0
         * @param pageID the ID of the new page
         * @return the new array
         */
        private Button[] makeButtonArray(Button[] base, int pageID) {
            var buttons = new Button[base.length];
            for (var i = 0; i < base.length; i++) {
                if (base[i] != null && base[i].page() == -1) {
                    buttons[i] = base[i];
                } else {
                    buttons[i] = Button.emptyButton(i, pageID, this.group);
                }
            }
            return buttons;
        }
    }

    /**
     * Holds the container buttons and the player inventory buttons of one page.
     */
    public static class ButtonPage {

        private final ButtonPageGroup group;
        private final int page;
        private final Button[] container;
        private final Button[] inventory;

        /**
         * Creates a page with the buttons.
         *
         * @param group the group of the page
         * @param page the ID of the page
         * @param container the container buttons
         * @param inventory the player inventory buttons
         */
        private ButtonPage(ButtonPageGroup group, int page, Button[] container, Button[] inventory) {
            this.group = group;
            this.page = page;
            this.container = container;
            this.inventory = inventory;
        }

        /**
         * Creates a page with empty container buttons. If the player is online and the options do not hide the
         * inventory, the inventory buttons show copies of the player items on all pages.
         *
         * @param group the group of the page
         * @param size the number of container slots
         * @param page the ID of the page
         * @param player the player that sees the page
         * @param options the options of the page
         */
        private ButtonPage(ButtonPageGroup group, int size, int page, PlayerReference player, PageOptions options) {
            this.group = group;
            this.page = page;
            this.container = Buttons.makeEmptyButtonArray(size, page);
            this.inventory = new Button[Inventory.INVENTORY_SIZE];
            player.entity().ifPresent(entity -> {
                var i = 0;
                for (var stack : entity.getInventory().items) {
                    int trueIndex = i < 9 ? i + 27 : i - 9;
                    if (options.isInventoryHidden() || stack.isEmpty()) {
                        this.inventory[trueIndex] = Button.emptyButton(trueIndex, page);
                    } else {
                        this.inventory[trueIndex] = Button.builder()
                                .item(stack.copy())
                                .index(trueIndex)
                                .page(-1)
                                .stackOverride()
                                .build();
                    }
                    i++;
                }
            });
        }

        /**
         * Runs the consumer for each container button, then for each player inventory button.
         *
         * @param counter the counter for the first button
         * @param group the group ID to give to the consumer
         * @param forEach the consumer to run
         * @return the counter that the last call returned
         */
        public int forEachButton(int counter, int group, ButtonConsumer forEach) {
            for (var button : this.container) {
                counter = forEach.accept(counter, button, group, this.page);
            }
            for (var button : this.inventory) {
                counter = forEach.accept(counter, button, group, this.page);
            }
            return counter;
        }

        /**
         * Gets the button at the index. Indexes after the container slots are player inventory slots.
         *
         * @param index the index of the button
         * @return the button, or a new empty button if the index is out of bounds
         */
        public Button getButton(int index) {
            if (this.isWithinBounds(index)) {
                if (index < this.container.length) {
                    return this.container[index];
                } else if (index < this.container.length + this.inventory.length) {
                    return this.inventory[index - this.container.length];
                }
            }
            return Button.emptyButton(index, this.page, this.group.group);
        }

        /**
         * Checks if the index is a container or player inventory slot of this page.
         *
         * @param index the index to check
         * @return {@code true} if the index is within the bounds
         */
        public boolean isWithinBounds(int index) {
            return index >= 0 && index < this.size();
        }

        /**
         * @return the number of container and player inventory slots
         */
        public int size() {
            return this.size(true);
        }

        /**
         * @param all {@code true} includes the player inventory slots
         * @return the number of slots
         */
        public int size(boolean all) {
            return this.container.length + (all ? this.inventory.length : 0);
        }
    }

    /**
     * Receives each button of a loop, and passes a counter from one button to the next.
     */
    @FunctionalInterface
    public interface ButtonConsumer {

        /**
         * @return the counter for the next button
         */
        int accept(int counter, Button button, int group, int page);
    }
}
