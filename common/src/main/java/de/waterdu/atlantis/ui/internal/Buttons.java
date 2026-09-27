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

    private static Button[] makeEmptyButtonArray(int size, int pageID) {
        var buttons = new Button[size];
        for (var i = 0; i < size; i++) {
            buttons[i] = Button.emptyButton(i, pageID);
        }
        return buttons;
    }

    public Button[] containerButtons() {
        return this.containerButtons(0, 0);
    }

    public Button[] containerButtons(int groupID, int pageID) {
        return this.groups.get(this.resolveGroup(groupID)).page(pageID).container;
    }

    public void setContainerButtons(Button[] buttons) {
        this.setContainerButtons(buttons, 0, 0);
    }

    public void setContainerButtons(Button[] buttons, int groupID, int pageID) {
        var groupButtons = this.groups.get(this.resolveGroup(groupID)).page(pageID).container;
        System.arraycopy(buttons, 0, groupButtons, 0, groupButtons.length);
    }

    public Button[] inventoryButtons() {
        return this.inventoryButtons(0, 0);
    }

    public Button[] inventoryButtons(int groupID, int pageID) {
        return this.groups.get(this.resolveGroup(groupID)).page(pageID).inventory;
    }

    public void setInventoryButtons(Button[] buttons) {
        this.setInventoryButtons(buttons, 0, 0);
    }

    public void setInventoryButtons(Button[] buttons, int groupID, int pageID) {
        var inventoryButtons = this.groups.get(this.resolveGroup(groupID)).page(pageID).inventory;
        System.arraycopy(buttons, 0, inventoryButtons, 0, inventoryButtons.length);
    }

    public int forEachButton(int counter, ButtonConsumer forEach) {
        for (var group : this.groups.values()) {
            counter = group.forEachButton(counter, forEach);
        }
        return counter;
    }

    public int forEachVisibleButton(int counter, ButtonConsumer forEach) {
        for (var group : this.groups.values()) {
            counter = group.currentPage().forEachButton(counter, group.group, forEach);
        }
        return counter;
    }

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

    public void forceInventoryDisplay(int windowID, int state, PlayerReference player) {
        player.entity().ifPresent(entity -> this.forEachVisibleButton(this.size(false), (counter, button, g, p) -> {
            entity.connection.send(new ClientboundContainerSetSlotPacket(windowID, state, counter++, button.display(player)));
            return counter;
        }));
    }

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

    public Button button(int index, int groupID, int pageID) {
        return this.button(index, this.group(groupID), pageID);
    }

    private Button button(int index, ButtonPageGroup group, int pageID) {
        return group.page(pageID).getButton(index);
    }

    public void setButton(int index, Button button) {
        this.setButton(index, 0, 0, button);
    }

    public void setButton(int index, int groupID, int pageID, Button button) {
        this.updateGroupCount(groupID);
        this.setButton(index, this.group(groupID), pageID, button);
    }

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

    public void clearButton(int index) {
        for (var group : this.groups.values()) {
            this.clearButton(index, group, group.currentPage);
        }
    }

    public void clearButton(int index, int groupID, int pageID) {
        this.clearButton(index, this.group(groupID), pageID);
    }

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

    public boolean isWithinBounds(int index) {
        return index >= 0 && index < this.size();
    }

    public int size() {
        return this.size(true);
    }

    public int size(boolean all) {
        var page = this.baseGroup.pageZero;
        return page.container.length + (all ? page.inventory.length : 0);
    }

    public NonNullList<ItemStack> asItemStacks(boolean all) {
        return this.asItemStacks(0, 0, all);
    }

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

    public int resolveGroup(int groupID) {
        return Math.floorMod(groupID, this.groups.size());
    }

    public ButtonPageGroup group(int groupID) {
        return this.groups.get(this.resolveGroup(groupID));
    }

    public ButtonPageGroup baseGroup() {
        return this.baseGroup;
    }

    public int groupCount() {
        return this.groups.size();
    }

    public static class ButtonPageGroup {

        private final int group;
        private final List<ButtonPage> pages;

        private final ButtonPage pageZero;

        private int currentPage;

        private ButtonPageGroup(int group, int size, PlayerReference player, PageOptions options) {
            this.group = group;
            this.pages = new ArrayList<>();
            this.currentPage = 0;
            this.pageZero = new ButtonPage(this, size, 0, player, options);
            this.pages.add(this.pageZero);
        }

        public int resolvePage(int page) {
            return Math.floorMod(page, this.pages.size());
        }

        public ButtonPage currentPage() {
            return this.page(this.currentPage);
        }

        public ButtonPage page(int page) {
            return this.pages.get(this.resolvePage(page));
        }

        public void setCurrentPage(int page) {
            this.currentPage = page;
        }

        public int currentPageID() {
            return this.currentPage;
        }

        public int pageCount() {
            return this.pages.size();
        }

        public int forEachButton(int counter, ButtonConsumer forEach) {
            for (var page : this.pages) {
                counter = page.forEachButton(counter, this.group, forEach);
            }
            return counter;
        }

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

        private Button[] makeContainerButtonArray(int pageID) {
            return this.makeButtonArray(this.pageZero.container, pageID);
        }

        private Button[] makeInventoryButtonArray(int pageID) {
            return this.makeButtonArray(this.pageZero.inventory, pageID);
        }

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

    public static class ButtonPage {

        private final ButtonPageGroup group;
        private final int page;
        private final Button[] container;
        private final Button[] inventory;

        private ButtonPage(ButtonPageGroup group, int page, Button[] container, Button[] inventory) {
            this.group = group;
            this.page = page;
            this.container = container;
            this.inventory = inventory;
        }

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

        public int forEachButton(int counter, int group, ButtonConsumer forEach) {
            for (var button : this.container) {
                counter = forEach.accept(counter, button, group, this.page);
            }
            for (var button : this.inventory) {
                counter = forEach.accept(counter, button, group, this.page);
            }
            return counter;
        }

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

        public boolean isWithinBounds(int index) {
            return index >= 0 && index < this.size();
        }

        public int size() {
            return this.size(true);
        }

        public int size(boolean all) {
            return this.container.length + (all ? this.inventory.length : 0);
        }
    }

    @FunctionalInterface
    public interface ButtonConsumer {

        int accept(int counter, Button button, int group, int page);
    }
}
