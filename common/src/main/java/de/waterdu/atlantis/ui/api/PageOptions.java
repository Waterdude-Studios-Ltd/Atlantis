package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.util.text.Text;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Holds the options of a page, such as its form factor, title, rows and refresh rate. Use {@link #builder()} to make
 * one.
 */
public class PageOptions {

    private static final PageOptions DEFAULTS = new PageOptions();
    private static final Consumer<Click> OUTSIDE_SCREEN_CLICK_SINK = __ -> {};

    private static final ResourceLocation COOKING_POT_ID = ResourceLocation.parse("pixelmon:cooking_pot");
    private static final ResourceLocation INFUSER_ID = ResourceLocation.parse("pixelmon:infuser");
    private static final ResourceLocation WASHING_MACHINE_ID = ResourceLocation.parse("pixelmon:washing_machine");
    private static final ResourceLocation POKE_BAG_ID = ResourceLocation.parse("pixelmon:poke_bag");
    private static final ResourceLocation DAY_CARE_ID = ResourceLocation.parse("pixelmon:day_care");

    private FormFactor formFactor = FormFactor.CHEST;
    private Text title = Text.empty();
    private int rows = 3;
    private boolean draggable = false;
    private long refreshRate = 1;
    private boolean inventoryHidden = false;
    private Consumer<Click> outsideScreenClick = OUTSIDE_SCREEN_CLICK_SINK;

    /**
     * @return the form factor of the page
     */
    public FormFactor formFactor() {
        return this.formFactor;
    }

    /**
     * @return the number of chest rows of the page
     */
    public int rows() {
        return this.rows;
    }

    /**
     * @return the title of the page
     */
    public Text title() {
        return this.title;
    }

    /**
     * @return {@code true} if players can move items in and out of the page
     */
    public boolean isDraggable() {
        return this.draggable;
    }

    /**
     * @return the number of ticks between page updates
     */
    public long refreshRate() {
        return this.refreshRate;
    }

    /**
     * @return always false for a draggable page
     */
    public boolean isInventoryHidden() {
        return this.inventoryHidden && !this.draggable;
    }

    /**
     * Runs the handler for a click outside the screen.
     *
     * @param click the click outside the screen
     */
    public void onClickOutsideScreen(Click click) {
        this.outsideScreenClick.accept(click);
    }

    /**
     * Gets the shared options with the default values: a chest of 3 rows with an empty title.
     *
     * @return the default options
     */
    public static PageOptions defaults() {
        return PageOptions.DEFAULTS;
    }

    /**
     * Calculates a hash code from all options.
     *
     * @return the hash code of the options
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.formFactor, this.title, this.rows, this.draggable, this.refreshRate, this.inventoryHidden, this.outsideScreenClick);
    }

    /**
     * Checks if the object is a {@link PageOptions} with the same options. The outside screen click handlers must be
     * the same instance.
     *
     * @param obj the object to compare
     * @return {@code true} if the options are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PageOptions other)) {
            return false;
        }
        return this.rows == other.rows
                && this.draggable == other.draggable
                && this.refreshRate == other.refreshRate
                && this.inventoryHidden == other.inventoryHidden
                && Objects.equals(this.formFactor, other.formFactor)
                && Objects.equals(this.title, other.title)
                && Objects.equals(this.outsideScreenClick, other.outsideScreenClick);
    }

    /**
     * Creates a builder that starts with the default values.
     *
     * @return the new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds {@link PageOptions}. Each setter changes the options and returns this builder.
     */
    public static class Builder {

        private final PageOptions options;

        /**
         * Creates a builder that starts with the default values.
         */
        public Builder() {
            this.options = new PageOptions();
        }

        /**
         * Sets the form factor of the page.
         *
         * @param formFactor the form factor
         * @return this builder
         */
        public Builder formFactor(FormFactor formFactor) {
            this.options.formFactor = formFactor;
            return this;
        }

        /**
         * @param rows the number of chest rows, from 1 to 6. Only form factors with rows use this value.
         */
        public Builder rows(int rows) {
            this.options.rows = rows;
            return this;
        }

        /**
         * Sets the title of the page.
         *
         * @param title the title, converted to {@link Text} with {@link TextUtils#asText}
         * @param <T> the type of the title
         * @return this builder
         */
        public <T> Builder title(T title) {
            this.options.title = TextUtils.asText(title);
            return this;
        }

        /**
         * Lets players move items in and out of the page as in a normal container. {@link Page#onChanged} receives
         * the changes. A draggable page always shows the player inventory.
         */
        public Builder draggable(boolean draggable) {
            this.options.draggable = draggable;
            return this;
        }

        /**
         * @param rate the number of ticks between page updates. Values below 2 update the page each tick.
         */
        public Builder refreshRate(long rate) {
            this.options.refreshRate = rate;
            return this;
        }

        /**
         * Sets if the page hides the player inventory. A draggable page always shows it.
         *
         * @param hidden {@code true} to hide the player inventory
         * @return this builder
         */
        public Builder inventoryHidden(boolean hidden) {
            this.options.inventoryHidden = hidden;
            return this;
        }

        /**
         * Hides the player inventory. A draggable page always shows it.
         *
         * @return this builder
         */
        public Builder hideInventory() {
            this.options.inventoryHidden = true;
            return this;
        }

        /**
         * Shows the player inventory.
         *
         * @return this builder
         */
        public Builder showInventory() {
            this.options.inventoryHidden = false;
            return this;
        }

        /**
         * @param outsideScreenClick runs off the server thread when the player clicks outside the screen
         */
        public Builder outsideScreenClick(Consumer<Click> outsideScreenClick) {
            this.options.outsideScreenClick = outsideScreenClick;
            return this;
        }

        /**
         * Gets the options. The builder does not copy them, so later changes to the builder also change them.
         *
         * @return the options
         */
        public PageOptions build() {
            return this.options;
        }
    }

    /**
     * Describes a type of container screen that a page can use.
     *
     * @param slots the number of container slots
     * @param hasRows {@code true} if the number of rows of the page changes the menu type
     * @param menuTypeFunction gets the menu type for the options of a page
     */
    public record FormFactor(
            int slots,
            boolean hasRows,
            Function<PageOptions, MenuType<? extends AbstractContainerMenu>> menuTypeFunction
    ) {

        public static FormFactor CHEST;
        public static FormFactor FURNACE;
        public static FormFactor ENCHANTING_TABLE;
        public static FormFactor VILLAGER_TRADE;
        public static FormFactor ANVIL;
        public static FormFactor CRAFTING_TABLE;
        public static FormFactor DISPENSER;
        public static FormFactor BREWING_STAND;
        public static FormFactor HOPPER;
        public static FormFactor BEACON;
        public static FormFactor BLAST_FURNACE;
        public static FormFactor GRINDSTONE;
        public static FormFactor LECTERN;
        public static FormFactor LOOM;
        public static FormFactor SHULKER_BOX;
        public static FormFactor SMITHING_TABLE;
        public static FormFactor SMOKER;
        public static FormFactor CARTOGRAPHY_TABLE;
        public static FormFactor STONECUTTER;

        public static FormFactor COOKING_POT;
        public static FormFactor INFUSER;
        public static FormFactor WASHING_MACHINE;
        public static FormFactor POKE_BAG;
        public static FormFactor DAY_CARE;

        /**
         * Creates a form factor.
         *
         * @param slots the number of container slots
         * @param hasRows {@code true} if the number of rows of the page changes the menu type
         * @param menuTypeFunction gets the menu type for the options of a page
         * @return the new form factor
         */
        public static FormFactor create(int slots, boolean hasRows, Function<PageOptions, MenuType<? extends AbstractContainerMenu>> menuTypeFunction) {
            return new FormFactor(slots, hasRows, menuTypeFunction);
        }

        static {
            FormFactor.CHEST = FormFactor.create(54, true, options -> switch (options.rows) {
                case 1 -> MenuType.GENERIC_9x1;
                case 2 -> MenuType.GENERIC_9x2;
                case 4 -> MenuType.GENERIC_9x4;
                case 5 -> MenuType.GENERIC_9x5;
                case 6 -> MenuType.GENERIC_9x6;
                default -> MenuType.GENERIC_9x3;
            });
            FormFactor.FURNACE = FormFactor.create(3, false, options -> MenuType.FURNACE);
            FormFactor.ENCHANTING_TABLE = FormFactor.create(2, false, options -> MenuType.ENCHANTMENT);
            FormFactor.VILLAGER_TRADE = FormFactor.create(3, false, options -> MenuType.MERCHANT);
            FormFactor.ANVIL = FormFactor.create(3, false, options -> MenuType.ANVIL);
            FormFactor.CRAFTING_TABLE = FormFactor.create(10, false, options -> MenuType.CRAFTING);
            FormFactor.DISPENSER = FormFactor.create(9, false, options -> MenuType.GENERIC_3x3);
            FormFactor.BREWING_STAND = FormFactor.create(5, false, options -> MenuType.BREWING_STAND);
            FormFactor.HOPPER = FormFactor.create(5, false, options -> MenuType.HOPPER);
            FormFactor.BEACON = FormFactor.create(1, false, options -> MenuType.BEACON);
            FormFactor.BLAST_FURNACE = FormFactor.create(3, false, options -> MenuType.BLAST_FURNACE);
            FormFactor.GRINDSTONE = FormFactor.create(3, false, options -> MenuType.GRINDSTONE);
            FormFactor.LECTERN = FormFactor.create(0, false, options -> MenuType.LECTERN);
            FormFactor.LOOM = FormFactor.create(4, false, options -> MenuType.LOOM);
            FormFactor.SHULKER_BOX = FormFactor.create(27, false, options -> MenuType.SHULKER_BOX);
            FormFactor.SMITHING_TABLE = FormFactor.create(3, false, options -> MenuType.SMITHING);
            FormFactor.SMOKER = FormFactor.create(3, false, options -> MenuType.SMOKER);
            FormFactor.CARTOGRAPHY_TABLE = FormFactor.create(3, false, options -> MenuType.CARTOGRAPHY_TABLE);
            FormFactor.STONECUTTER = FormFactor.create(2, false, options -> MenuType.STONECUTTER);
        }

        /**
         * Creates the Pixelmon form factors, such as {@link #COOKING_POT}. If Pixelmon is not loaded, they stay null.
         */
        public static void createDefaults(HolderLookup.Provider registryAccess) {
            // TODO: Mod proxy this.
            if (Mods.isPresent("pixelmon")) {
                FormFactor.COOKING_POT = FormFactor.create(11, false, options -> BuiltInRegistries.MENU.getOptional(COOKING_POT_ID).orElse(null));
                FormFactor.INFUSER = FormFactor.create(4, false, options -> BuiltInRegistries.MENU.getOptional(INFUSER_ID).orElse(null));
                FormFactor.WASHING_MACHINE = FormFactor.create(2, false, options -> BuiltInRegistries.MENU.getOptional(WASHING_MACHINE_ID).orElse(null));
                FormFactor.POKE_BAG = FormFactor.create(27, false, options -> BuiltInRegistries.MENU.getOptional(POKE_BAG_ID).orElse(null));
                FormFactor.DAY_CARE = FormFactor.create(0, false, options -> BuiltInRegistries.MENU.getOptional(DAY_CARE_ID).orElse(null));
            }
        }

        /**
         * Gets the menu type for the options of a page.
         *
         * @param options the options of the page
         * @return the menu type, or {@code null} if a Pixelmon menu is not registered
         */
        public MenuType<? extends AbstractContainerMenu> getType(PageOptions options) {
            return this.menuTypeFunction.apply(options);
        }
    }
}
