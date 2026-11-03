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

/**
 * Edits the decorations of a page. A left click paints the brush item into a slot, a middle click clears the slot,
 * and a right click opens the {@link DecorationPalette}.
 */
public final class DecorationEditor implements Page {

    final Class<? extends Page> clazz;
    final String name;
    final ItemStack brush;
    final int title;
    final Decorations decorations;
    int ticker;

    /**
     * Creates an editor with an empty brush. If the page has no decorations yet, it creates, puts and saves new
     * ones.
     *
     * @param clazz the page class to decorate
     */
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

    /**
     * Creates an editor with the given state.
     *
     * @param clazz the page class to decorate
     * @param name the name to show in the title
     * @param brush the item to paint, or {@link ItemStack#EMPTY} to clear slots
     * @param title the index of the title to show. Values above {@code 3} become {@code 0}.
     * @param decorations the decorations to edit
     * @param ticker the number of updates since the title changed
     */
    public DecorationEditor(Class<? extends Page> clazz, String name, ItemStack brush, int title, Decorations decorations, int ticker) {
        this.clazz = clazz;
        this.name = name;
        this.brush = brush;
        this.title = title > 3 ? 0 : title;
        this.decorations = decorations;
        this.ticker = ticker;
    }

    /**
     * Adds the current decorations as buttons.
     *
     * @param player the player that views the page
     * @param buttons the collector for the buttons
     */
    @Override
    public void addButtons(PlayerReference player, ButtonCollector buttons) {
        buttons.collect(this.decorations.buttons());
    }

    /**
     * Gets the options of the editor. It uses the rows and form factor that the decorated page last opened with,
     * and hides the inventory.
     *
     * @param player the player that views the page
     * @return the page options
     */
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

    /**
     * Opens the palette on a right click. On other clicks in a slot, it paints the brush into the slot, or clears
     * the slot if the brush is empty or the click is a middle click. Then it opens the editor again.
     *
     * @param player the player that clicked
     * @param index the index of the clicked slot. Negative values change no slot.
     * @param click the click
     */
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

    /**
     * Counts the updates. After more than 45 updates, it opens a new editor that shows the next title.
     *
     * @param tick the current tick
     * @param player the player that views the page
     * @param buttons the current buttons
     * @return the buttons, unchanged
     */
    @Override
    public Buttons onUpdate(long tick, PlayerReference player, Buttons buttons) {
        this.ticker++;
        if (this.ticker > 45) {
            AtlantisUI.open(player, new DecorationEditor(this.clazz, this.name, this.brush, this.title + 1, this.decorations, 0));
        }
        return buttons;
    }

    /**
     * Saves the decorations, marks them as changed and maps the decorations again.
     *
     * @param player the player that closed the page
     */
    @Override
    public void afterClose(PlayerReference player) {
        this.decorations.save();
        this.decorations.setChanged();
        AtlantisData.mapDecorations();
    }

    /**
     * Gets the title for the current title index: the page name, the brush, or one of two usage hints.
     *
     * @return the title
     */
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
