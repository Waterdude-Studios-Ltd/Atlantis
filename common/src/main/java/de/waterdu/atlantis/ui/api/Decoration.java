package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Map;

public class Decoration extends Button {

    public Decoration(ItemStack item, int index) {
        super(item, index);
        this.setStackOverride(false);
        this.setLowPriority(true);
    }

    public Decoration(Block block, int index) {
        super(new ItemStack(block), index);
        this.setStackOverride(false);
        this.setLowPriority(true);
    }

    public Decoration(Item item, int index) {
        super(new ItemStack(item), index);
        this.setStackOverride(false);
        this.setLowPriority(true);
    }

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

    public static Decoration from(String decoration) {
        return new Decoration(decoration);
    }

    public static Decoration from(Map.Entry<Integer, ConfigurableItemStack> decoration) {
        return new Decoration(decoration);
    }
}
