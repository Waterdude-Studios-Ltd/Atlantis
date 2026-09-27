package de.waterdu.atlantis.self.decoration;

import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.ui.api.Button;
import de.waterdu.atlantis.ui.api.ButtonCollector;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record DecorationPalette(DecorationEditor parent) implements Page {

    private static final Text LORE_A = Text.of(ChatFormatting.GRAY + "Left click to select brush");
    private static final Text LORE_B = Text.of(ChatFormatting.GRAY + "Right click to pin brush");
    private static final Text LORE_C = Text.of(ChatFormatting.RED + "Right click to unpin brush");

    @Override
    public PageOptions options(PlayerReference player) {
        return PageOptions.builder()
                .title("Choose a brush")
                .rows(6)
                .inventoryHidden(true)
                .build();
    }

    @Override
    public void addButtons(PlayerReference player, ButtonCollector buttons) {
        buttons.collect(
                Button.builder().index(64).item(Items.ARROW).name(ChatFormatting.YELLOW + "<-").prevPage().build(),
                Button.builder().index(70).item(Items.ARROW).name(ChatFormatting.YELLOW + "->").nextPage().build(),
                Button.builder()
                        .index(67)
                        .item(Items.BARRIER)
                        .name(ChatFormatting.WHITE + "Clear")
                        .loreLine(DecorationPalette.LORE_A)
                        .onLeftClick(click ->
                                click.open(
                                        new DecorationEditor(this.parent.clazz, this.parent.name, ItemStack.EMPTY, 0, this.parent.decorations, 0)
                                )
                        )
                        .build()
        );

        var pins = Settings.get().getParsedPinnedDecorations();

        var page = 0;
        var slot = 0;

        for (var item : pins) {
            if (item == Items.AIR) {
                continue;
            }
            this.addItemButton(buttons, item, true, page, slot++);
            if (slot >= 54) {
                slot = 0;
                page++;
            }
        }

        for (var item : Registries.ITEM.registryFromRegistryKey()) {
            if (item == Items.AIR) {
                continue;
            }
            this.addItemButton(buttons, item, false, page, slot++);
            if (slot >= 54) {
                slot = 0;
                page++;
            }
        }
    }

    private void addItemButton(ButtonCollector buttons, Item item, boolean pinned, int page, int slot) {
        var stack = new ItemStack(item);
        var name = Text.of(ChatFormatting.RESET + "" + ChatFormatting.WHITE + item.getName(stack).getString());
        buttons.collect(Button.builder()
                .item(stack)
                .name(name)
                .index(slot)
                .page(page)
                .singlePress()
                .loreLine(DecorationPalette.LORE_A)
                .loreLine(pinned ? DecorationPalette.LORE_C : DecorationPalette.LORE_B)
                .onLeftClick(click ->
                        click.open(new DecorationEditor(this.parent.clazz, this.parent.name, stack, 0, this.parent.decorations, 0))
                )
                .onRightClick(click -> {
                    var settings = Settings.get();
                    if (pinned) {
                        settings.getPinnedDecorations().removeIf(c -> c.item().equals(item));
                    } else {
                        settings.getPinnedDecorations().add(ConfigurableItemStack.of(item));
                    }
                    settings.save();
                    click.open(this);
                })
                .build()
        );
    }
}
