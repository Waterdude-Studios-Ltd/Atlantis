package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Map;

/**
 * Shows an item in a page slot. A decoration is low priority, so it does not replace a non-empty button at the same
 * index.
 */
public class Decoration extends Button {

    /**
     * Creates a decoration that shows an item stack.
     *
     * @param item the item stack to show
     * @param index the slot index
     */
    public Decoration(ItemStack item, int index) {
        super(item, index);
        this.setStackOverride(false);
        this.setLowPriority(true);
    }

    /**
     * Creates a decoration that shows a block.
     *
     * @param block the block to show
     * @param index the slot index
     */
    public Decoration(Block block, int index) {
        super(new ItemStack(block), index);
        this.setStackOverride(false);
        this.setLowPriority(true);
    }

    /**
     * Creates a decoration that shows an item.
     *
     * @param item the item to show
     * @param index the slot index
     */
    public Decoration(Item item, int index) {
        super(new ItemStack(item), index);
        this.setStackOverride(false);
        this.setLowPriority(true);
    }

    /**
     * Creates a decoration with no name, no lore, no effect and hidden tooltips from a string.
     *
     * @param decoration the slot index and the item, separated by one space
     */
    private Decoration(String decoration) {
        this.setStackOverride(false);
        this.setHideTooltips(true);
        this.setHideAllTooltips(true);
        this.setHasEffect(false);
        this.setName("");
        this.setLore(new ArrayList<>());
        this.setEmpty(false);
        var split = decoration.split(" ");
        if (split.length == 2) {
            this.setIndex(Integer.parseInt(split[0]));
            this.setItem(ConfigurableItemStack.of(TextUtils.concat(1, " ", split)).stack());
        }
        this.setLowPriority(true);
    }

    /**
     * Creates a decoration with no name, no lore, no effect and hidden tooltips from a map entry.
     *
     * @param decoration the slot index and the item
     */
    private Decoration(Map.Entry<Integer, ConfigurableItemStack> decoration) {
        this.setStackOverride(false);
        this.setHideTooltips(true);
        this.setHideAllTooltips(true);
        this.setHasEffect(false);
        this.setName("");
        this.setLore(new ArrayList<>());
        this.setEmpty(false);
        this.setItem(decoration.getValue());
        this.setIndex(decoration.getKey());
        this.setLowPriority(true);
    }

    /**
     * @param decoration the slot index and the item, separated by one space, for example {@code 4 minecraft:stone}. The
     *     item must not contain spaces. Other formats give a decoration with no item at slot 0.
     */
    public static Decoration from(String decoration) {
        return new Decoration(decoration);
    }

    /**
     * Creates a decoration with no name, no lore, no effect and hidden tooltips.
     *
     * @param decoration the slot index and the item
     * @return the new decoration
     */
    public static Decoration from(Map.Entry<Integer, ConfigurableItemStack> decoration) {
        return new Decoration(decoration);
    }
}
