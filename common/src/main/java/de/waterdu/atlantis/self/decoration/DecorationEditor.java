package de.waterdu.atlantis.self.decoration;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.AtlantisData;
import de.waterdu.atlantis.ui.api.AtlantisUI;
import de.waterdu.atlantis.ui.api.ButtonCollector;
import de.waterdu.atlantis.ui.api.Click;
import de.waterdu.atlantis.ui.api.Decorations;
import de.waterdu.atlantis.ui.api.Page;
import de.waterdu.atlantis.ui.api.PageOptions;
import de.waterdu.atlantis.ui.internal.Buttons;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.world.item.ItemStack;

public final class DecorationEditor implements Page {

    final Class<? extends Page> clazz;
    final String name;
    final ItemStack brush;
    final int title;
    final Decorations decorations;
    int ticker;

    public DecorationEditor(Class<? extends Page> clazz) {
        this.clazz = clazz;
        this.name = clazz.getSimpleName();
        this.brush = ItemStack.EMPTY;
        this.title = 0;
        var decorations = AtlantisData.decorations(clazz);
        if (decorations == null) {
            decorations = new Decorations(clazz.getName());
            Atlantis.CONFIG.put(Decorations.class, decorations);
            decorations.save();
        }
        this.decorations = decorations;
        this.ticker = 0;
    }

    public DecorationEditor(Class<? extends Page> clazz, String name, ItemStack brush, int title, Decorations decorations, int ticker) {
        this.clazz = clazz;
        this.name = name;
        this.brush = brush;
        this.title = title > 3 ? 0 : title;
        this.decorations = decorations;
        this.ticker = ticker;
    }

    @Override
    public void addButtons(PlayerReference player, ButtonCollector buttons) {
        buttons.collect(this.decorations.buttons());
    }

    @Override
    public PageOptions options(PlayerReference player) {
        var formFactor = Atlantis.listeners().formFactor(this.clazz);
        return PageOptions.builder()
                .inventoryHidden(true)
                .rows(formFactor.getA())
                .formFactor(formFactor.getB())
                .title(this.getTitle())
                .build();
    }

    @Override
    public void onButtonClick(PlayerReference player, int index, Click click) {
        if (click.button() == Click.Button.RIGHT) {
            new DecorationPalette(this).open(player);
        } else if (index >= 0) {
            if (this.brush.isEmpty() || click.button() == Click.Button.MIDDLE) {
                this.decorations.decorations().remove(index);
            } else {
                this.decorations.decorations().put(index, ConfigurableItemStack.of(this.brush.getItem()));
            }
            this.decorations.setChanged();
            this.open(player);
        }
    }

    @Override
    public Buttons onUpdate(long tick, PlayerReference player, Buttons buttons) {
        this.ticker++;
        if (this.ticker > 45) {
            AtlantisUI.open(player, new DecorationEditor(this.clazz, this.name, this.brush, this.title + 1, this.decorations, 0));
        }
        return buttons;
    }

    @Override
    public void afterClose(PlayerReference player) {
        this.decorations.save();
        this.decorations.setChanged();
        AtlantisData.mapDecorations();
    }

    private Text getTitle() {
        if (this.title == 0) {
            return Text.of("Editing: " + this.name);
        } else if (this.title == 1) {
            return Text.of("Brush: " + (this.brush.isEmpty() ? "Clear" : this.brush.getItem().getName(this.brush).getString()));
        } else if (this.title == 2) {
            return Text.of("Right click to change brush");
        } else {
            return Text.of("Middle click to clear");
        }
    }
}
