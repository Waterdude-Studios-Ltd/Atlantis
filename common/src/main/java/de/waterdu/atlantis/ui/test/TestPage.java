package de.waterdu.atlantis.ui.test;

import de.waterdu.atlantis.ui.api.Button;
import de.waterdu.atlantis.ui.api.ButtonCollector;
import de.waterdu.atlantis.ui.api.Click;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public class TestPage implements Page {

    private static final Item[] ITEMS = new Item[]{
            Items.OAK_PLANKS,
            Items.CRAFTING_TABLE,
            Items.WOODEN_PICKAXE,
            Items.COBBLESTONE,
            Items.STONE_PICKAXE,
            Items.IRON_ORE,
            Items.IRON_INGOT,
            Items.IRON_PICKAXE,
            Items.DIAMOND,
            Items.DIAMOND_PICKAXE,
    };

    @Override
    public PageOptions options(PlayerReference player) {
        return PageOptions.builder()
                .title("Test Page")
                .rows(3)
                .inventoryHidden(true)
                .build();
    }

    @Override
    public void addButtons(PlayerReference player, ButtonCollector buttons) {
        var cis = ConfigurableItemStack.of("#sprite Pikachu gender:male shiny #end glow");
        var item = cis.newStack();
        var tag = new CompoundTag();
        tag.putInt("test", 1);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        buttons.collect(Button.builder().item(item).name("&eSprite Test").index(9));
        for (var page = 0; page < 10; page++) {
            for (var i = 0; i < 9; i++) {
                buttons.collect(Button.builder()
                        .name("&r&fGroup " + (i + 1) + ", page " + (page + 1))
                        .item(ConfigurableItemStack.of(ITEMS[page]))
                        .hideTooltips()
                        .index(i)
                        .page(page)
                        .group(i)
                        .onLeftClick(Click::next)
                        .onRightClick(Click::previous)
                        .build()
                );
            }
        }
    }
}
