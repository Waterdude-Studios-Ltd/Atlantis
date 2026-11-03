package de.waterdu.atlantis.ui.api;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.ui.internal.AtlantisContainerMenuInternal;
import de.waterdu.atlantis.ui.internal.Buttons;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Entrypoint;
import de.waterdu.atlantis.util.concurrency.Concurrency;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.item.ItemUtils;
import de.waterdu.atlantis.util.java.interfaces.TriConsumer;
import de.waterdu.atlantis.util.text.Text;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Buttons make up the contents of a {@link Page}.
 * You can create buttons when a page opens for a player, or you can create and keep them in advance.
 *
 * @author Sam Plummer sam@waterdu.de
 * @version 1.0
 * @since 02/03/2019
 */
public class Button {

    /**
     * UUID that identifies this button.
     */
    private final UUID uuid = UUID.randomUUID();

    /**
     * Name of this button. Shown as the first line of the button.
     */
    private Text name;

    /**
     * Name of this button as a {@link Component}, for example to use translations.
     */
    private Component directName = null;

    /**
     * Lore of this button. Shown as subsequent lines of the button.
     */
    private List<Text> lore;

    /**
     * Lore of this button as {@link Component}s, for example to use translations.
     */
    private List<Component> directLore = null;

    /**
     * Tooltip of this button.
     */
    private String tooltip = "";

    /**
     * Item that the UI displays for this button.
     */
    private ItemStack item;

    /**
     * Whether to hide tooltips on the button.
     */
    private boolean hideTooltips;

    /**
     * Whether to hide all tooltips on the button.
     */
    private boolean hideAllTooltips;

    /**
     * Whether to display this button exactly as the given stack. If true, the button ignores the name and lore.
     */
    private boolean stackOverride;

    /**
     * Whether to apply HasEffect to the button.
     */
    private boolean hasEffect;

    /**
     * Index at which the {@link Page} shows this button.
     */
    private int index;

    /**
     * Group that this button is for.
     */
    private int group = 0;

    /**
     * Page that this button is for.
     * -1 means all pages.
     */
    private int page = -1;

    /**
     * Number of repeat presses that are allowed in the current page session.
     * Integer.MIN_VALUE means no limit.
     */
    private int presses = Integer.MIN_VALUE;

    /**
     * Guard predicates and tasks to run when this button is clicked.
     */
    private final List<OnClickTask> onClickTasks = new ArrayList<>();

    /**
     * Whether the task runs asynchronously or synchronously.
     */
    private boolean async = true;

    /**
     * Whether to update this button on the next tick.
     */
    private boolean changed = true;

    /**
     * Whether this button represents nothing.
     */
    private boolean empty = false;

    /**
     * Whether this button has a low priority.
     */
    private boolean lowPriority = false;

    /**
     * Button update callbacks.
     */
    private final List<ButtonUpdateTask> updateTasks = new ArrayList<>();

    /**
     * Contexts to pass to placeholders.
     */
    private final List<Object> placeholderContexts = new ArrayList<>();

    /**
     * Constructs a new button.
     *
     * @param name {@link Button#name}
     * @param lore {@link Button#lore}
     * @param item {@link Button#item}
     * @param index {@link Button#index}
     */
    protected Button(Text name, List<Text> lore, ItemStack item, int index) {
        this(name, lore, item, index, true, false);
    }

    /**
     * Constructs a new button with legacy options.
     *
     * @param name {@link Button#name}
     * @param lore {@link Button#lore}
     * @param item {@link Button#item}
     * @param index {@link Button#index}
     */
    protected Button(String name, String lore, ItemStack item, int index) {
        this.name = Text.of(name);
        this.lore = new ArrayList<>();
        if (lore != null) {
            for (String line : lore.split("\\n")) {
                this.lore.add(Text.of(line));
            }
        }
        this.item = item;
        this.index = index;
        this.stackOverride = false;
        this.hideTooltips = true;
        this.hasEffect = false;
    }

    /**
     * Constructs a new button.
     *
     * @param name {@link Button#name}
     * @param lore {@link Button#lore}
     * @param item {@link Button#item}
     * @param index {@link Button#index}
     * @param hideTooltips {@link Button#hideTooltips}
     * @param hasEffect {@link Button#hasEffect}
     */
    protected Button(Text name, List<Text> lore, ItemStack item, int index, boolean hideTooltips, boolean hasEffect) {
        this.name = name;
        this.lore = lore;
        this.item = item;
        this.index = index;
        this.stackOverride = false;
        this.hideTooltips = hideTooltips;
        this.hasEffect = hasEffect;
    }

    /**
     * Constructs a button with a stack override.
     *
     * @param item {@link Button#item}
     * @param index {@link Button#index}
     */
    protected Button(ItemStack item, int index) {
        this.name = Text.empty();
        this.lore = new ArrayList<>();
        this.item = item;
        this.index = index;
        this.stackOverride = true;
        this.hideTooltips = false;
        this.hasEffect = false;
    }

    /**
     * Constructs an empty button at index 0, with no item, name or lore.
     */
    protected Button() {
        this.name = Text.empty();
        this.lore = new ArrayList<>();
        this.index = 0;
        this.stackOverride = false;
        this.hideTooltips = false;
        this.hasEffect = false;
        this.empty = true;
    }

    /**
     * Creates the ItemStack that the UI displays for this button.
     *
     * @return display of this button
     * @see ItemStack
     */
    public ItemStack display(PlayerReference player) {
        if (this.item == null || this.item.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (this.stackOverride) {
            return this.item;
        }

        var placeholders = Settings.get().isUsePlaceholdersInPages();

        if (this.directName != null) {
            ItemUtils.setDisplayName(this.item, this.directName);
        } else {
            if (placeholders) {
                if (!this.placeholderContexts.contains(player)) {
                    this.placeholderContexts.add(player);
                }
                ItemUtils.setDisplayNameWithPlaceholders(this.item, this.name, this.placeholderContexts);
            } else {
                ItemUtils.setDisplayName(this.item, this.name);
            }
        }

        if (this.directLore != null) {
            if (!this.directLore.isEmpty()) {
                ItemUtils.setDisplayLore(this.item, this.directLore);
            }
        } else {
            if (!this.lore.isEmpty()) {
                if (placeholders) {
                    ItemUtils.setDisplayLoreWithPlaceholders(this.item, this.placeholderContexts, this.lore);
                } else {
                    ItemUtils.setDisplayLore(this.item, this.lore);
                }
            }
        }

        ItemUtils.setPixelmonTooltip(this.item, this.tooltip);

        if (this.hasEffect) {
            ItemUtils.applyGlow(this.item);
        }

        if (this.hideTooltips) {
            ItemUtils.hideTooltips(this.item);
        }

        if (this.hideAllTooltips) {
            ItemUtils.hideAllTooltips(this.item);
        }

        this.item.sanitizeComponents();

        return this.item;
    }

    /**
     * @return the index at which the page shows this button
     */
    public int index() {
        return this.index;
    }

    /**
     * Marks this button as changed, so that it updates on the next tick.
     *
     * @return this button
     */
    public Button markDirty() {
        this.changed = true;
        return this;
    }

    /**
     * Sets the index at which the page shows this button, and marks the button as changed.
     *
     * @param index the slot index
     * @return this button
     */
    public Button setIndex(int index) {
        this.index = index;
        return this.markDirty();
    }

    /**
     * Sets the page group of this button. Negative values become 0.
     *
     * @param group the page group
     * @return this button
     */
    public Button setGroup(int group) {
        this.group = Math.max(0, group);
        return this;
    }

    /**
     * @param page the page in the group. -1 shows the button on all pages.
     */
    public Button setPage(int page) {
        this.page = page;
        return this.markDirty();
    }

    /**
     * Checks if this button shows on the current page of its group.
     *
     * @param buttons the buttons that hold the page groups
     * @return {@code true} if the button shows on all pages or on the current page of its group
     */
    public boolean isVisible(Buttons buttons) {
        return this.isVisible(buttons.group(this.group));
    }

    /**
     * Checks if this button shows on the current page of the given group.
     *
     * @param group the page group, or {@code null}
     * @return {@code true} if the button shows on all pages, or if the group is not {@code null} and its current
     *     page is the page of this button
     */
    public boolean isVisible(Buttons.ButtonPageGroup group) {
        return this.page == -1 || (group != null && this.page == group.currentPageId());
    }

    /**
     * @param presses the number of presses that the button accepts. {@link Integer#MIN_VALUE} means no limit.
     */
    public Button setPresses(int presses) {
        this.presses = presses;
        return this.markDirty();
    }

    /**
     * Sets the name of this button.
     *
     * @param name the name, as any value that {@link TextUtils#asText(Object)} accepts
     * @param <T> the type of the name
     * @return this button
     */
    public <T> Button setName(T name) {
        this.name = TextUtils.asText(name);
        return this.markDirty();
    }

    /**
     * Takes priority over the name from {@link #setName(Object)}. Placeholders do not apply to it.
     */
    public Button setDirectName(Component name) {
        this.directName = name;
        return this.markDirty();
    }

    /**
     * Removes the direct name, so that the button uses the name from {@link #setName(Object)} again.
     *
     * @return this button
     */
    public Button removeDirectName() {
        this.directName = null;
        return this.markDirty();
    }

    /**
     * Hides the attribute modifiers and the additional tooltip lines of the item. The name and lore stay visible.
     */
    public Button setHideTooltips(boolean hideTooltips) {
        this.hideTooltips = hideTooltips;
        return this.markDirty();
    }

    /**
     * Hides the whole tooltip, including the name and lore.
     */
    public Button setHideAllTooltips(boolean hideAllTooltips) {
        this.hideAllTooltips = hideAllTooltips;
        return this.markDirty();
    }

    /**
     * Removes all lines of the lore.
     *
     * @return this button
     */
    public Button clearLore() {
        this.lore.clear();
        return this.markDirty();
    }

    /**
     * Splits the text into lines at each {@code \n}.
     */
    public <T> Button setLore(T lore) {
        this.clearLore();
        if (lore != null) {
            for (var line : TextUtils.asString(lore).split("\\n")) {
                this.lore.add(Text.of(line));
            }
        }
        return this.markDirty();
    }

    /**
     * Replaces the lore with the given lines.
     *
     * @param lore the lines, or {@code null} to clear the lore
     * @param <T> the type of the lines
     * @return this button
     */
    public <T> Button setLore(List<T> lore) {
        this.clearLore();
        if (lore != null) {
            for (var line : lore) {
                this.lore.add(TextUtils.asText(line));
            }
        }
        return this.markDirty();
    }

    /**
     * Adds the lines to the end of the lore.
     *
     * @param lore the lines to add
     * @param <T> the type of the lines
     * @return this button
     */
    @SafeVarargs
    public final <T> Button addLore(T... lore) {
        if (lore != null) {
            for (var line : lore) {
                this.lore.add(TextUtils.asText(line));
            }
        }
        return this.markDirty();
    }

    /**
     * Adds a line to the end of the lore.
     *
     * @param line the line to add
     * @param <T> the type of the line
     * @return this button
     */
    public <T> Button addLoreLine(T line) {
        this.lore.add(TextUtils.asText(line));
        return this.markDirty();
    }

    /**
     * Uses {@link Text#raw()}, so color codes in the line are ignored.
     */
    public <T> Button addLoreLineRaw(T line) {
        this.lore.add(TextUtils.asText(line).raw());
        return this.markDirty();
    }

    /**
     * Creates an empty direct lore list if the button has none.
     */
    private void setupDirectLore() {
        if (this.directLore == null) {
            this.directLore = new ArrayList<>();
        }
    }

    /**
     * Removes the direct lore, so that the button uses the lore from {@link #setLore(List)} again.
     *
     * @return this button
     */
    public Button removeDirectLore() {
        this.directLore = null;
        return this.markDirty();
    }

    /**
     * Removes all lines of the direct lore. The empty direct lore still takes priority over the lore.
     *
     * @return this button
     */
    public Button clearDirectLore() {
        this.setupDirectLore();
        this.directLore.clear();
        return this.markDirty();
    }

    /**
     * Replaces the direct lore with one line.
     *
     * @param lore the line
     * @return this button
     */
    public Button setDirectLore(Component lore) {
        this.clearDirectLore();
        this.directLore.add(lore);
        return this.markDirty();
    }

    /**
     * Takes priority over the lore from {@link #setLore(List)}. Placeholders do not apply to it.
     */
    public Button setDirectLore(List<Component> lore) {
        this.clearDirectLore();
        if (lore != null) {
            this.directLore.addAll(lore);
        }
        return this.markDirty();
    }

    /**
     * Adds the lines to the end of the direct lore.
     *
     * @param lore the lines to add
     * @return this button
     */
    public Button addDirectLore(Component... lore) {
        this.setupDirectLore();
        if (lore != null) {
            this.directLore.addAll(Arrays.asList(lore));
        }
        return this.markDirty();
    }

    /**
     * Adds a line to the end of the direct lore.
     *
     * @param line the line to add
     * @return this button
     */
    public Button addDirectLoreLine(Component line) {
        this.setupDirectLore();
        this.directLore.add(line);
        return this.markDirty();
    }

    /**
     * Stores the text in the {@code tooltip} custom data tag of the item, which Pixelmon shows.
     */
    public Button setTooltip(String tooltip) {
        this.tooltip = tooltip;
        return this.markDirty();
    }

    /**
     * Sets whether the item of this button glows.
     *
     * @param hasEffect {@code true} to make the item glow
     * @return this button
     */
    public Button setHasEffect(boolean hasEffect) {
        this.hasEffect = hasEffect;
        return this.markDirty();
    }

    /**
     * Sets the stack that the UI displays for this button. The button changes this stack when it is displayed.
     *
     * @param item the stack
     * @return this button
     */
    public Button setItem(ItemStack item) {
        this.item = item;
        return this.markDirty();
    }

    /**
     * Sets the item that the UI displays for this button, with a count of 1.
     *
     * @param item the item
     * @return this button
     */
    public Button setItem(Item item) {
        return this.setItem(item, 1);
    }

    /**
     * Sets the item that the UI displays for this button.
     *
     * @param item the item
     * @param count the stack size
     * @return this button
     */
    public Button setItem(Item item, int count) {
        return this.setItem(new ItemStack(item, count));
    }

    /**
     * Sets the item of the block as the item that the UI displays for this button, with a count of 1.
     *
     * @param block the block
     * @return this button
     */
    public Button setItem(Block block) {
        return this.setItem(Item.BY_BLOCK.get(block));
    }

    /**
     * Sets the item of the block as the item that the UI displays for this button.
     *
     * @param block the block
     * @param count the stack size
     * @return this button
     */
    public Button setItem(Block block, int count) {
        return this.setItem(Item.BY_BLOCK.get(block), count);
    }

    /**
     * Parses the item string with {@link ConfigurableItemStack#of(String)} and sets the result as the item.
     *
     * @param item the item string
     * @return this button
     */
    public Button setItem(String item) {
        return this.setItem(ConfigurableItemStack.of(item));
    }

    /**
     * Parses the item string with {@link ConfigurableItemStack#of(String)} and sets the result as the item.
     *
     * @param item the item string
     * @param count the stack size
     * @return this button
     */
    public Button setItem(String item, int count) {
        return this.setItem(ConfigurableItemStack.of(item), count);
    }

    /**
     * Sets a new stack of the configurable item as the item.
     *
     * @param item the configurable item
     * @return this button
     */
    public Button setItem(ConfigurableItemStack item) {
        return this.setItem(item.newStack());
    }

    /**
     * Sets a new stack of the configurable item as the item.
     *
     * @param item the configurable item
     * @param count the stack size
     * @return this button
     */
    public Button setItem(ConfigurableItemStack item, int count) {
        return this.setItem(item.newStack(count));
    }

    /**
     * @param stackOverride true shows the item as it is. The name, lore, effect and tooltip settings are ignored.
     */
    public Button setStackOverride(boolean stackOverride) {
        this.stackOverride = stackOverride;
        return this.markDirty();
    }

    /**
     * Replaces all click tasks with one task that runs for each click.
     *
     * @param onClick the task
     * @return this button
     */
    public Button setOnClick(Consumer<Click> onClick) {
        this.onClickTasks.clear();
        this.onClickTasks.add(new OnClickTask(onClick));
        return this.markDirty();
    }

    /**
     * Replaces all click tasks with one task that runs for each click.
     *
     * @param onClick the task, which also receives the page
     * @return this button
     */
    public Button setOnClick(BiConsumer<Page, Click> onClick) {
        this.onClickTasks.clear();
        this.onClickTasks.add(new OnClickTask(onClick));
        return this.markDirty();
    }

    /**
     * Adds a task that runs for each click.
     *
     * @param onClick the task
     * @return this button
     */
    public Button addOnClick(Consumer<Click> onClick) {
        this.onClickTasks.add(new OnClickTask(onClick));
        return this.markDirty();
    }

    /**
     * Adds a task that runs for each click.
     *
     * @param onClick the task, which also receives the page
     * @return this button
     */
    public Button addOnClick(BiConsumer<Page, Click> onClick) {
        this.onClickTasks.add(new OnClickTask(onClick));
        return this.markDirty();
    }

    /**
     * Runs the click tasks on the thread that handles the click, instead of in a new task. This thread is an
     * Atlantis thread pool thread, not the server thread.
     */
    public Button sync() {
        this.async = false;
        return this;
    }

    /**
     * Runs the click tasks in a new task on the Atlantis thread pool. This is the default.
     *
     * @return this button
     */
    public Button async() {
        this.async = true;
        return this;
    }

    /**
     * Removes all click tasks.
     *
     * @return this button
     */
    public Button clearOnClick() {
        this.onClickTasks.clear();
        return this.markDirty();
    }

    /**
     * Marks this button as unchanged, so that it does not update on the next tick.
     *
     * @return this button
     */
    public Button markClean() {
        this.changed = false;
        return this;
    }

    /**
     * A low-priority button does not replace a non-empty button at the same index.
     */
    public Button setLowPriority() {
        this.lowPriority = true;
        return this;
    }

    /**
     * Gives this button the standard priority, so that it replaces any button at the same index.
     *
     * @return this button
     */
    public Button setStandardPriority() {
        this.lowPriority = false;
        return this;
    }

    /**
     * @return the name of this button
     */
    public Text name() {
        return this.name;
    }

    /**
     * @return the direct name of this button, or {@code null} if it has none
     */
    public Component directName() {
        return this.directName;
    }

    /**
     * @return {@code true} if this button has a direct name
     */
    public boolean hasDirectName() {
        return this.directName != null;
    }

    /**
     * @return the lore of this button. Changes to the list change the button.
     */
    public List<Text> lore() {
        return this.lore;
    }

    /**
     * @return the direct lore of this button, or {@code null} if it has none
     */
    public List<Component> directLore() {
        return this.directLore;
    }

    /**
     * @return {@code true} if this button has a direct lore, also if it is empty
     */
    public boolean hasDirectLore() {
        return this.directLore != null;
    }

    /**
     * @return the Pixelmon tooltip text of this button
     */
    public String tooltip() {
        return this.tooltip;
    }

    /**
     * @return the stack that the UI displays for this button, or {@code null} if it has none
     */
    public ItemStack item() {
        return this.item;
    }

    /**
     * @return {@code true} if the button shows the item as it is
     */
    public boolean isStackOverride() {
        return this.stackOverride;
    }

    /**
     * @return {@code true} if the item of this button glows
     */
    public boolean hasEffect() {
        return this.hasEffect;
    }

    /**
     * @return the page group of this button
     */
    public int group() {
        return this.group;
    }

    /**
     * @return the page of this button in its group, or -1 for all pages
     */
    public int page() {
        return this.page;
    }

    /**
     * @return the number of presses that the button still accepts, or {@link Integer#MIN_VALUE} for no limit
     */
    public int presses() {
        return this.presses;
    }

    /**
     * @return {@code true} if the attribute modifiers and the additional tooltip lines of the item are hidden
     */
    public boolean hideTooltips() {
        return this.hideTooltips;
    }

    /**
     * @return {@code true} if the whole tooltip is hidden
     */
    public boolean hideAllTooltips() {
        return this.hideAllTooltips;
    }

    /**
     * Adds the given amount to the number of presses that the button accepts. It does not mark the button as
     * changed.
     *
     * @param presses the amount to add. A negative amount removes presses.
     * @return this button
     */
    public Button changePresses(int presses) {
        this.presses += presses;
        return this;
    }

    /**
     * @return the click tasks of this button. Changes to the list change the button.
     */
    public List<OnClickTask> onClickTasks() {
        return this.onClickTasks;
    }

    /**
     * Runs the click tasks of this button for the click. For the button outside the screen, it runs the
     * outside-screen handler of the page options instead.
     *
     * @param async {@code true} to run the tasks in a new task on the Atlantis thread pool
     * @param page the page that was clicked
     * @param click the click
     */
    public void performOnClick(boolean async, Page page, Click click) {
        if (async) {
            Concurrency.async(() -> this.performOnClickInternal(page, click));
        } else {
            this.performOnClickInternal(page, click);
        }
    }

    /**
     * Runs the click tasks of this button on the current thread. For the button outside the screen, it runs the
     * outside-screen handler of the page options instead.
     *
     * @param page the page that was clicked
     * @param click the click
     */
    private void performOnClickInternal(Page page, Click click) {
        if (this.index == AtlantisContainerMenuInternal.OUTSIDE_SCREEN_INDEX) {
            page.options(click.player()).onClickOutsideScreen(click);
        } else {
            for (var task : this.onClickTasks) {
                task.perform(page, click);
            }
        }
    }

    /**
     * @return {@code true} if the click tasks run in a new task on the Atlantis thread pool
     */
    public boolean isAsync() {
        return this.async;
    }

    /**
     * Sets whether the click tasks run in a new task. See {@link #sync()} and {@link #async()}.
     *
     * @param async {@code true} to run the click tasks in a new task
     * @return this button
     */
    public Button setAsync(boolean async) {
        this.async = async;
        return this;
    }

    /**
     * @return {@code true} if this button updates on the next tick
     */
    public boolean isChanged() {
        return this.changed;
    }

    /**
     * Sets whether this button updates on the next tick.
     *
     * @param changed {@code true} to update the button on the next tick
     * @return this button
     */
    public Button setChanged(boolean changed) {
        this.changed = changed;
        return this;
    }

    /**
     * @return {@code true} if this button represents nothing
     */
    public boolean isEmpty() {
        return this.empty;
    }

    /**
     * Returns whether the button is not empty. It does not check the current page. See {@link #isVisible(Buttons)}.
     */
    public boolean isVisible() {
        return !this.isEmpty();
    }

    /**
     * Sets whether this button represents nothing.
     *
     * @param empty {@code true} if the button represents nothing
     * @return this button
     */
    public Button setEmpty(boolean empty) {
        this.empty = empty;
        return this;
    }

    /**
     * @return {@code true} if this button has a low priority
     */
    public boolean isLowPriority() {
        return this.lowPriority;
    }

    /**
     * Sets whether this button has a low priority. See {@link #setLowPriority()}.
     *
     * @param lowPriority {@code true} to give the button a low priority
     * @return this button
     */
    public Button setLowPriority(boolean lowPriority) {
        this.lowPriority = lowPriority;
        return this;
    }

    /**
     * @return the update tasks of this button. Changes to the list change the button.
     */
    public List<ButtonUpdateTask> updateTasks() {
        return this.updateTasks;
    }

    /**
     * Adds an update task to this button.
     *
     * @param updateTask the task to add
     * @return this button
     */
    public Button addUpdateTask(ButtonUpdateTask updateTask) {
        this.updateTasks.add(updateTask);
        return this.markDirty();
    }

    /**
     * Removes an update task from this button.
     *
     * @param updateTask the task to remove
     * @return this button
     */
    public Button removeUpdateTask(ButtonUpdateTask updateTask) {
        this.updateTasks.remove(updateTask);
        return this.markDirty();
    }

    /**
     * Removes all update tasks from this button.
     *
     * @return this button
     */
    public Button clearUpdateTasks() {
        this.updateTasks.clear();
        return this.markDirty();
    }

    /**
     * Replaces the update tasks of this button with the update tasks of another button. Both buttons then share the
     * same task instances.
     *
     * @param button the button to copy the tasks from
     * @return this button
     */
    public Button copyUpdateTasks(Button button) {
        this.updateTasks.clear();
        this.updateTasks.addAll(button.updateTasks);
        return this.markDirty();
    }

    /**
     * Placeholder contexts only apply if placeholders in pages are enabled. The viewing player is added automatically.
     */
    public Button addPlaceholderContext(Object context) {
        this.placeholderContexts.add(context);
        return this.markDirty();
    }

    /**
     * Removes all placeholder contexts, including the viewing players that were added.
     *
     * @return this button
     */
    public Button clearPlaceholderContexts() {
        this.placeholderContexts.clear();
        return this.markDirty();
    }

    /**
     * Checks if the other button has the same index, page and group as this button.
     *
     * @param button the other button, or {@code null}
     * @return {@code true} if both buttons are in the same place
     */
    public boolean isSamePlace(Button button) {
        return button != null && this.index() == button.index() && this.page() == button.page() && this.group() == button.group();
    }

    /**
     * Gets the hash code of the UUID of this button.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    /**
     * Checks if the other object is a button with the same UUID.
     *
     * @param obj the other object
     * @return {@code true} if the object is the same button
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Button)) {
            return false;
        }
        return this.uuid.equals(((Button) obj).uuid);
    }

    /**
     * Gets a string with the UUID of this button.
     *
     * @return the string, in the format {@code Button(<uuid>)}
     */
    @Override
    public String toString() {
        return "Button(" + this.uuid + ")";
    }

    /**
     * Creates a new button builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param item an Item, ItemStack, Block or ConfigurableItemStack. Other values are converted with
     *     {@link String#valueOf(Object)} and parsed as an item string.
     */
    public static Builder of(Object name, Object item, int index) {
        Builder builder = Button.builder().name(name).index(index);
        if (item instanceof Item) {
            builder.item((Item) item);
        } else if (item instanceof ItemStack) {
            builder.item((ItemStack) item);
        } else if (item instanceof Block) {
            builder.item((Block) item);
        } else if (item instanceof ConfigurableItemStack) {
            builder.item((ConfigurableItemStack) item);
        } else {
            builder.item(String.valueOf(item));
        }
        return builder;
    }

    /**
     * Creates a builder with the name, lore, item and index set. See {@link #of(Object, Object, int)}.
     *
     * @param name the name
     * @param lore the lore. It is split into lines at each {@code \n}.
     * @param item an Item, ItemStack, Block or ConfigurableItemStack, or an item string
     * @param index the slot index
     * @return the builder
     */
    public static Builder of(Object name, Object lore, Object item, int index) {
        return Button.of(name, item, index).lore(lore);
    }

    /**
     * Builds a {@link Button}. Each builder holds one button and changes it directly.
     */
    public static final class Builder {

        private final Button button;

        /**
         * Constructs a builder for a new, non-empty button that hides tooltips.
         */
        public Builder() {
            this.button = new Button();
            this.button.empty = false;
            this.button.hideTooltips = true;
        }

        /**
         * Sets the name of the button.
         *
         * @param name the name, as any value that {@link TextUtils#asText(Object)} accepts
         * @param <T> the type of the name
         * @return this builder
         */
        public <T> Builder name(T name) {
            this.button.name = TextUtils.asText(name);
            return this;
        }

        /**
         * Uses {@link Text#raw()}, so color codes in the name are ignored.
         */
        public <T> Builder rawName(T name) {
            this.button.name = TextUtils.asText(name).raw();
            return this;
        }

        /**
         * Takes priority over the name from {@link #name(Object)}. Placeholders do not apply to it.
         */
        public Builder directName(Component name) {
            this.button.directName = name;
            return this;
        }
        
        /**
         * Removes the direct name, so that the button uses the name from {@link #name(Object)}.
         *
         * @return this builder
         */
        public Builder removeDirectName() {
            this.button.directName = null;
            return this;
        }

        /**
         * Removes all lines of the lore.
         *
         * @return this builder
         */
        public Builder clearLore() {
            this.button.lore.clear();
            return this;
        }

        /**
         * Splits the text into lines at each {@code \n}.
         */
        public <T> Builder lore(T lore) {
            this.clearLore();
            if (lore != null) {
                for (String line : TextUtils.asString(lore).split("\\n")) {
                    this.button.lore.add(Text.of(line));
                }
            }
            return this;
        }

        /**
         * Replaces the lore with the given lines.
         *
         * @param lore the lines, or {@code null} to clear the lore
         * @param <T> the type of the lines
         * @return this builder
         */
        public <T> Builder lore(List<T> lore) {
            this.clearLore();
            if (lore != null) {
                for (T line : lore) {
                    this.button.lore.add(TextUtils.asText(line));
                }
            }
            return this;
        }

        /**
         * Adds the lines to the current lore. Unlike the other {@code lore} methods, it does not clear the lore first.
         */
        @SafeVarargs
        public final <T> Builder lore(T... lore) {
            if (lore != null) {
                for (T line : lore) {
                    this.button.lore.add(TextUtils.asText(line));
                }
            }
            return this;
        }

        /**
         * Adds a line to the end of the lore.
         *
         * @param line the line to add
         * @param <T> the type of the line
         * @return this builder
         */
        public <T> Builder loreLine(T line) {
            this.button.lore.add(TextUtils.asText(line));
            return this;
        }

        /**
         * Adds an empty line to the end of the lore.
         *
         * @return this builder
         */
        public Builder blankLoreLine() {
            return this.loreLine("");
        }

        /**
         * Uses {@link Text#raw()}, so color codes in the line are ignored.
         */
        public <T> Builder rawLoreLine(T line) {
            this.button.lore.add(TextUtils.asText(line).raw());
            return this;
        }

        /**
         * Removes all lines of the direct lore. The empty direct lore still takes priority over the lore.
         *
         * @return this builder
         */
        public Builder clearDirectLore() {
            this.button.setupDirectLore();
            this.button.directLore.clear();
            return this;
        }

        /**
         * Replaces the direct lore with one line.
         *
         * @param lore the line, or {@code null} to only clear the direct lore
         * @return this builder
         */
        public Builder directLore(Component lore) {
            this.clearDirectLore();
            if (lore != null) {
                this.button.directLore.add(lore);
            }
            return this;
        }

        /**
         * Takes priority over the lore from {@link #lore(List)}. Placeholders do not apply to it.
         */
        public Builder directLore(List<Component> lore) {
            this.clearDirectLore();
            if (lore != null) {
                this.button.directLore.addAll(lore);
            }
            return this;
        }
        
        /**
         * Replaces the direct lore with the given lines.
         *
         * @param lore the lines
         * @return this builder
         */
        public Builder directLore(Component... lore) {
            return this.directLore(Arrays.asList(lore));
        }

        /**
         * Adds a line to the end of the direct lore.
         *
         * @param line the line to add
         * @return this builder
         */
        public Builder directLoreLine(Component line) {
            this.button.setupDirectLore();
            this.button.directLore.add(line);
            return this;
        }

        /**
         * Adds an empty line to the end of the direct lore.
         *
         * @return this builder
         */
        public Builder blankDirectLoreLine() {
            return this.directLoreLine(Component.literal(""));
        }
        
        /**
         * Removes the direct lore, so that the button uses the lore from {@link #lore(List)}.
         *
         * @return this builder
         */
        public Builder removeDirectLore() {
            this.button.directLore = null;
            return this;
        }

        /**
         * Stores the text in the {@code tooltip} custom data tag of the item, which Pixelmon shows.
         */
        public Builder tooltip(String tooltip) {
            this.button.tooltip = tooltip;
            return this;
        }

        /**
         * Sets the item that the UI displays for the button, with a count of 1.
         *
         * @param item the item
         * @return this builder
         */
        public Builder item(Item item) {
            return this.item(item, 1);
        }

        /**
         * Sets the item that the UI displays for the button.
         *
         * @param item the item
         * @param count the stack size
         * @return this builder
         */
        public Builder item(Item item, int count) {
            return this.item(new ItemStack(item, count));
        }

        /**
         * Sets the item of the block as the item that the UI displays for the button, with a count of 1.
         *
         * @param block the block
         * @return this builder
         */
        public Builder item(Block block) {
            return this.item(Item.BY_BLOCK.get(block));
        }

        /**
         * Sets the item of the block as the item that the UI displays for the button.
         *
         * @param block the block
         * @param count the stack size
         * @return this builder
         */
        public Builder item(Block block, int count) {
            return this.item(Item.BY_BLOCK.get(block), count);
        }

        /**
         * Parses the item string with {@link ConfigurableItemStack#of(String)} and sets the result as the item.
         *
         * @param item the item string
         * @return this builder
         */
        public Builder item(String item) {
            return this.item(ConfigurableItemStack.of(item));
        }

        /**
         * Parses the item string with {@link ConfigurableItemStack#of(String)} and sets the result as the item.
         *
         * @param item the item string
         * @param count the stack size
         * @return this builder
         */
        public Builder item(String item, int count) {
            return this.item(ConfigurableItemStack.of(item), count);
        }

        /**
         * Sets a new stack of the configurable item as the item.
         *
         * @param item the configurable item
         * @return this builder
         */
        public Builder item(ConfigurableItemStack item) {
            return this.item(item.newStack());
        }

        /**
         * Sets a new stack of the configurable item as the item.
         *
         * @param item the configurable item
         * @param count the stack size
         * @return this builder
         */
        public Builder item(ConfigurableItemStack item, int count) {
            return this.item(item.newStack(count));
        }

        /**
         * Sets the stack that the UI displays for the button. The button changes this stack when it is displayed.
         *
         * @param stack the stack
         * @return this builder
         */
        public Builder item(ItemStack stack) {
            this.button.item = stack;
            return this;
        }

        /**
         * Shows the item as it is. The name, lore, effect and tooltip settings are ignored.
         *
         * @return this builder
         */
        public Builder stackOverride() {
            return this.stackOverride(true);
        }

        /**
         * @param stackOverride true shows the item as it is. The name, lore, effect and tooltip settings are ignored.
         */
        public Builder stackOverride(boolean stackOverride) {
            this.button.stackOverride = stackOverride;
            return this;
        }

        /**
         * Makes the item of the button glow.
         *
         * @return this builder
         */
        public Builder effect() {
            return this.effect(true);
        }

        /**
         * Sets whether the item of the button glows.
         *
         * @param hasEffect {@code true} to make the item glow
         * @return this builder
         */
        public Builder effect(boolean hasEffect) {
            this.button.hasEffect = hasEffect;
            return this;
        }

        /**
         * Sets the index at which the page shows the button.
         *
         * @param index the slot index
         * @return this builder
         */
        public Builder index(int index) {
            this.button.index = index;
            return this;
        }

        /**
         * Sets the page group of the button. Unlike {@link Button#setGroup(int)}, it does not change negative values.
         *
         * @param group the page group
         * @return this builder
         */
        public Builder group(int group) {
            this.button.group = group;
            return this;
        }

        /**
         * @param page the page in the group. -1 shows the button on all pages. This is the default.
         */
        public Builder page(int page) {
            this.button.page = page;
            return this;
        }

        /**
         * @param presses the number of presses that the button accepts. {@link Integer#MIN_VALUE} means no limit.
         */
        public Builder presses(int presses) {
            this.button.presses = presses;
            return this;
        }

        /**
         * Makes the button accept only one press.
         *
         * @return this builder
         */
        public Builder singlePress() {
            return this.presses(1);
        }

        /**
         * Hides the attribute modifiers and the additional tooltip lines of the item.
         *
         * @return this builder
         */
        public Builder hideTooltips() {
            return this.hideTooltips(true);
        }

        /**
         * Shows the attribute modifiers and the additional tooltip lines of the item.
         *
         * @return this builder
         */
        public Builder showTooltips() {
            return this.hideTooltips(false);
        }

        /**
         * Hides the attribute modifiers and the additional tooltip lines of the item. The name and lore stay visible.
         * Builders hide them by default.
         */
        public Builder hideTooltips(boolean hideTooltips) {
            this.button.hideTooltips = hideTooltips;
            return this;
        }

        /**
         * Hides the whole tooltip, including the name and lore.
         *
         * @return this builder
         */
        public Builder hideAllTooltips() {
            return this.hideAllTooltips(true);
        }

        /**
         * Stops hiding the whole tooltip.
         *
         * @return this builder
         */
        public Builder showAllTooltips() {
            return this.hideAllTooltips(false);
        }

        /**
         * Hides the whole tooltip, including the name and lore.
         */
        public Builder hideAllTooltips(boolean hideAllTooltips) {
            this.button.hideAllTooltips = hideAllTooltips;
            return this;
        }

        /**
         * Starts a click task that runs only for some mouse buttons, styles or rates.
         *
         * @return a new click builder that returns to this builder
         */
        public OnClickBuilder onClick() {
            return new OnClickBuilder(this);
        }

        /**
         * Does not run for the second click of a double click.
         */
        public Builder onLeftClick(Consumer<Click> onClick) {
            return new OnClickBuilder(this).leftClick(onClick);
        }

        /**
         * Adds a task that runs for single, normal right clicks.
         *
         * @param onClick the task
         * @return this builder
         */
        public Builder onRightClick(Consumer<Click> onClick) {
            return new OnClickBuilder(this).rightClick(onClick);
        }

        /**
         * Adds a task that runs for single shift left clicks.
         *
         * @param onClick the task
         * @return this builder
         */
        public Builder onShiftLeftClick(Consumer<Click> onClick) {
            return new OnClickBuilder(this).shiftLeftClick(onClick);
        }

        /**
         * Adds a task that runs for single shift right clicks.
         *
         * @param onClick the task
         * @return this builder
         */
        public Builder onShiftRightClick(Consumer<Click> onClick) {
            return new OnClickBuilder(this).shiftRightClick(onClick);
        }

        /**
         * Adds a task that runs for the second click of a normal left double click.
         *
         * @param onClick the task
         * @return this builder
         */
        public Builder onLeftDoubleClick(Consumer<Click> onClick) {
            return new OnClickBuilder(this).leftDoubleClick(onClick);
        }

        /**
         * Runs for each click, with any mouse button, style or rate.
         */
        public Builder onClick(Consumer<Click> onClick) {
            this.button.onClickTasks.add(new OnClickTask(onClick));
            return this;
        }

        /**
         * Adds a task that runs for each click that passes the guard.
         *
         * @param onClickGuard the guard, or {@code null} to run for each click
         * @param onClick the task
         * @return this builder
         */
        public Builder onClick(Predicate<Click> onClickGuard, Consumer<Click> onClick) {
            this.button.onClickTasks.add(new OnClickTask(onClickGuard, onClick));
            return this;
        }

        /**
         * Adds a task that runs for each click.
         *
         * @param onClick the task, which also receives the page
         * @return this builder
         */
        public Builder onClick(BiConsumer<Page, Click> onClick) {
            this.button.onClickTasks.add(new OnClickTask(onClick));
            return this;
        }

        /**
         * Adds a left click task that opens the next page.
         *
         * @return this builder
         */
        public Builder nextPage() {
            return this.onLeftClick(Click.NEXT_PAGE);
        }

        /**
         * Adds a left click task that opens the previous page.
         *
         * @return this builder
         */
        public Builder prevPage() {
            return this.onLeftClick(Click.PREVIOUS_PAGE);
        }

        /**
         * Adds a left click task that closes the page.
         *
         * @return this builder
         */
        public Builder closePage() {
            return this.onLeftClick(Click.CLOSE_PAGE);
        }

        /**
         * See {@link Button#sync()}.
         */
        public Builder sync() {
            this.button.sync();
            return this;
        }

        /**
         * See {@link Button#async()}.
         *
         * @return this builder
         */
        public Builder async() {
            this.button.async();
            return this;
        }

        /**
         * A low-priority button does not replace a non-empty button at the same index.
         */
        public Builder lowPriority() {
            this.button.setLowPriority();
            return this;
        }

        /**
         * See {@link Button#setStandardPriority()}.
         *
         * @return this builder
         */
        public Builder standardPriority() {
            this.button.setStandardPriority();
            return this;
        }

        /**
         * Adds an update task that runs each tick, with no delay.
         *
         * @param task receives the button, the game time and the task
         * @return this builder
         */
        public Builder updateTask(TriConsumer<Button, Long, ButtonUpdateTask> task) {
            return this.updateTask(1, task);
        }

        /**
         * Adds an update task with no delay.
         *
         * @param tickrate see {@link ButtonUpdateTask#setTickrate(int)}
         * @param task receives the button, the game time and the task
         * @return this builder
         */
        public Builder updateTask(int tickrate, TriConsumer<Button, Long, ButtonUpdateTask> task) {
            return this.updateTask(tickrate, 0, task);
        }

        /**
         * @param tickrate see {@link ButtonUpdateTask#setTickrate(int)}
         * @param delay see {@link ButtonUpdateTask#setDelay(int)}
         * @param task receives the button, the game time and the task
         */
        public Builder updateTask(int tickrate, int delay, TriConsumer<Button, Long, ButtonUpdateTask> task) {
            this.button.addUpdateTask(new ButtonUpdateTask(tickrate, delay, task));
            return this;
        }

        /**
         * Placeholder contexts only apply if placeholders in pages are enabled. The viewing player is added
         * automatically.
         */
        public Builder placeholderContexts(Object... contexts) {
            this.button.placeholderContexts.addAll(Lists.newArrayList(contexts));
            return this;
        }

        /**
         * Runs the task on this builder if the condition is true.
         *
         * @param doIf the condition
         * @param task the task to run on this builder
         * @return this builder
         */
        public Builder doIf(boolean doIf, Consumer<Builder> task) {
            if (doIf) {
                task.accept(this);
            }
            return this;
        }

        /**
         * Runs the task on this builder if the supplier returns true. The supplier is checked once, now.
         *
         * @param doIf supplies the condition
         * @param task the task to run on this builder
         * @return this builder
         */
        public Builder doIf(BooleanSupplier doIf, Consumer<Builder> task) {
            return this.doIf(doIf.getAsBoolean(), task);
        }

        /**
         * Returns the same button on each call. Later changes to this builder also change that button.
         */
        public Button build() {
            return this.button;
        }
    }

    /**
     * Holds a task that runs when a button is clicked, and an optional guard that decides if it runs.
     */
    public static final class OnClickTask {

        private final Predicate<Click> guard;
        private final Either<Consumer<Click>, BiConsumer<Page, Click>> onClick;

        /**
         * Constructs a task that runs for each click.
         *
         * @param onClick the task
         */
        public OnClickTask(Consumer<Click> onClick) {
            this(null, onClick);
        }

        /**
         * Constructs a task that runs for each click that passes the guard.
         *
         * @param guard the guard, or {@code null} to run for each click
         * @param onClick the task
         */
        public OnClickTask(Predicate<Click> guard, Consumer<Click> onClick) {
            this.guard = guard;
            this.onClick = Either.left(onClick);
        }

        /**
         * Constructs a task that runs for each click.
         *
         * @param onClick the task, which also receives the page
         */
        public OnClickTask(BiConsumer<Page, Click> onClick) {
            this(null, onClick);
        }

        /**
         * Constructs a task that runs for each click that passes the guard.
         *
         * @param guard the guard, or {@code null} to run for each click
         * @param onClick the task, which also receives the page
         */
        public OnClickTask(Predicate<Click> guard, BiConsumer<Page, Click> onClick) {
            this.guard = guard;
            this.onClick = Either.right(onClick);
        }

        /**
         * Checks if the click passes the guard.
         *
         * @param data the click
         * @return {@code true} if there is no guard or the guard accepts the click
         */
        public boolean testGuard(Click data) {
            return this.guard == null || this.guard.test(data);
        }

        /**
         * Runs the task if the click passes the guard.
         *
         * @param page the page that was clicked
         * @param data the click
         */
        public void perform(Page page, Click data) {
            if (this.testGuard(data)) {
                this.onClick.ifLeft(onClick -> onClick.accept(data)).ifRight(onClick -> onClick.accept(page, data));
            }
        }
    }

    /**
     * Builds a click task that runs only for clicks with the set mouse button, style and rate.
     */
    @Bootstrapped
    public static final class OnClickBuilder {

        private static final Map<Long, Predicate<Click>> GUARD_PREDICATES = new HashMap<>();

        /**
         * Creates a guard predicate for each combination of mouse button, click style and click rate, including
         * {@code null} values that match any click.
         */
        @Entrypoint
        @SuppressWarnings("unused")
        private static void bootstrap() {
            for (var mouseButton : Click.Button.valuesAndNull()) {
                for (var clickStyle : Click.Style.valuesAndNull()) {
                    for (var clickRate : Click.Rate.valuesAndNull()) {
                        GUARD_PREDICATES.put(guardHash(mouseButton, clickStyle, clickRate), click -> {
                            if (mouseButton != null && !mouseButton.equals(click.button())) {
                                return false;
                            }

                            if (clickStyle != null && !clickStyle.equals(click.style())) {
                                return false;
                            }

                            return clickRate == null || clickRate.equals(click.rate());
                        });
                    }
                }
            }
        }

        /**
         * Gets the key of the guard predicate for a combination of mouse button, click style and click rate.
         *
         * @param mouseButton the mouse button, or {@code null} for any
         * @param clickStyle the click style, or {@code null} for any
         * @param clickRate the click rate, or {@code null} for any
         * @return the key
         */
        private static long guardHash(Click.Button mouseButton, Click.Style clickStyle, Click.Rate clickRate) {
            var hash = 7;
            hash = 31 * hash + (mouseButton == null ? 1 : mouseButton.hashCode());
            hash = 31 * hash + (clickStyle == null ? 1 : clickStyle.hashCode());
            hash = 31 * hash + (clickRate == null ? 1 : clickRate.hashCode());
            return hash;
        }

        private final Builder parent;

        private Click.Button mouseButton;
        private Click.Style clickStyle;
        private Click.Rate clickRate;

        /**
         * Constructs a click builder that adds its task to the parent builder.
         *
         * @param parent the button builder
         */
        private OnClickBuilder(Builder parent) {
            this.parent = parent;
        }

        /**
         * Adds a task that runs for single, normal left clicks.
         *
         * @param onClick the task
         * @return the parent builder
         */
        public Builder leftClick(Consumer<Click> onClick) {
            return this.left().normal().singleClick().action(onClick);
        }

        /**
         * Adds a task that runs for single, normal right clicks.
         *
         * @param onClick the task
         * @return the parent builder
         */
        public Builder rightClick(Consumer<Click> onClick) {
            return this.right().normal().singleClick().action(onClick);
        }

        /**
         * Adds a task that runs for single shift left clicks.
         *
         * @param onClick the task
         * @return the parent builder
         */
        public Builder shiftLeftClick(Consumer<Click> onClick) {
            return this.left().shift().singleClick().action(onClick);
        }

        /**
         * Adds a task that runs for single shift right clicks.
         *
         * @param onClick the task
         * @return the parent builder
         */
        public Builder shiftRightClick(Consumer<Click> onClick) {
            return this.right().shift().singleClick().action(onClick);
        }

        /**
         * Adds a task that runs for the second click of a normal left double click.
         *
         * @param onClick the task
         * @return the parent builder
         */
        public Builder leftDoubleClick(Consumer<Click> onClick) {
            return this.left().normal().doubleClick().action(onClick);
        }

        /**
         * Matches only the left mouse button.
         *
         * @return this click builder
         */
        public OnClickBuilder left() {
            return this.mouseButton(Click.Button.LEFT);
        }

        /**
         * Matches only the right mouse button.
         *
         * @return this click builder
         */
        public OnClickBuilder right() {
            return this.mouseButton(Click.Button.RIGHT);
        }

        /**
         * Matches only the middle mouse button.
         *
         * @return this click builder
         */
        public OnClickBuilder middle() {
            return this.mouseButton(Click.Button.MIDDLE);
        }

        /**
         * Sets the mouse button to match.
         *
         * @param mouseButton the mouse button, or {@code null} for any
         * @return this click builder
         */
        public OnClickBuilder mouseButton(Click.Button mouseButton) {
            this.mouseButton = mouseButton;
            return this;
        }

        /**
         * Matches only normal clicks.
         *
         * @return this click builder
         */
        public OnClickBuilder normal() {
            return this.clickStyle(Click.Style.NORMAL);
        }

        /**
         * Matches only shift clicks.
         *
         * @return this click builder
         */
        public OnClickBuilder shift() {
            return this.clickStyle(Click.Style.SHIFT);
        }

        /**
         * Matches only drag clicks.
         *
         * @return this click builder
         */
        public OnClickBuilder drag() {
            return this.clickStyle(Click.Style.DRAG);
        }

        /**
         * Sets the click style to match.
         *
         * @param clickStyle the click style, or {@code null} for any
         * @return this click builder
         */
        public OnClickBuilder clickStyle(Click.Style clickStyle) {
            this.clickStyle = clickStyle;
            return this;
        }

        /**
         * The second click of a double click does not match.
         */
        public OnClickBuilder singleClick() {
            return this.clickRate(Click.Rate.SINGLE);
        }

        /**
         * Matches only the second click of a double click.
         *
         * @return this click builder
         */
        public OnClickBuilder doubleClick() {
            return this.clickRate(Click.Rate.DOUBLE);
        }

        /**
         * Sets the click rate to match.
         *
         * @param clickRate the click rate, or {@code null} for any
         * @return this click builder
         */
        public OnClickBuilder clickRate(Click.Rate clickRate) {
            this.clickRate = clickRate;
            return this;
        }

        /**
         * Runs the action only for clicks that match the set mouse button, style and rate. Values that are not set
         * match any click.
         */
        public Builder action(Consumer<Click> onClick) {
            return this.parent.onClick(GUARD_PREDICATES.get(guardHash(this.mouseButton, this.clickStyle, this.clickRate)), onClick);
        }
    }

    /**
     * Creates an empty button at index 0.
     *
     * @return the empty button
     */
    public static Button emptyButton() {
        return new Button();
    }

    /**
     * Creates an empty button at index 0.
     *
     * @param async {@code true} to run click tasks in a new task
     * @return the empty button
     */
    public static Button emptyButton(boolean async) {
        var button = new Button();
        button.setAsync(async);
        return button;
    }

    /**
     * Creates an empty button at the given index.
     *
     * @param index the slot index
     * @return the empty button
     */
    public static Button emptyButton(int index) {
        var button = new Button();
        button.setIndex(index);
        return button;
    }

    /**
     * Creates an empty button at the given index and page.
     *
     * @param index the slot index
     * @param page the page in the group, or -1 for all pages
     * @return the empty button
     */
    public static Button emptyButton(int index, int page) {
        var button = Button.emptyButton(index);
        button.setPage(page);
        return button;
    }

    /**
     * Creates an empty button at the given index, page and group.
     *
     * @param index the slot index
     * @param page the page in the group, or -1 for all pages
     * @param group the page group. Negative values become 0.
     * @return the empty button
     */
    public static Button emptyButton(int index, int page, int group) {
        var button = Button.emptyButton(index);
        button.setPage(page);
        button.setGroup(group);
        return button;
    }
}