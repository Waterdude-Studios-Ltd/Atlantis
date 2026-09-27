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

import java.util.function.Consumer;
import java.util.function.Function;

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

    private transient int hashCode = -1;

    public FormFactor formFactor() {
        return this.formFactor;
    }

    public int rows() {
        return this.rows;
    }

    public Text title() {
        return this.title;
    }

    public boolean isDraggable() {
        return this.draggable;
    }

    public long refreshRate() {
        return this.refreshRate;
    }

    public boolean isInventoryHidden() {
        return this.inventoryHidden && !this.draggable;
    }

    public void onClickOutsideScreen(Click click) {
        this.outsideScreenClick.accept(click);
    }

    public static PageOptions defaults() {
        return PageOptions.DEFAULTS;
    }

    @Override
    public int hashCode() {
        if (this.hashCode == -1) {
            this.hashCode = Long.hashCode(Long.parseLong(
                    this.rows + "" +
                            this.title.get().length() +
                            this.formFactor.slots +
                            (this.draggable ? "1" : "0") +
                            this.refreshRate +
                            (this.inventoryHidden ? "1" : "0") +
                            this.outsideScreenClick.hashCode()
            ) + this.title.hashCode());
        }
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PageOptions)) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final PageOptions options;

        public Builder() {
            this.options = new PageOptions();
        }

        public Builder formFactor(FormFactor formFactor) {
            this.options.formFactor = formFactor;
            return this;
        }

        public Builder rows(int rows) {
            this.options.rows = rows;
            return this;
        }

        public <T> Builder title(T title) {
            this.options.title = TextUtils.asText(title);
            return this;
        }

        public Builder draggable(boolean draggable) {
            this.options.draggable = draggable;
            return this;
        }

        public Builder refreshRate(long rate) {
            this.options.refreshRate = rate;
            return this;
        }

        public Builder inventoryHidden(boolean hidden) {
            this.options.inventoryHidden = hidden;
            return this;
        }

        public Builder hideInventory() {
            this.options.inventoryHidden = true;
            return this;
        }

        public Builder showInventory() {
            this.options.inventoryHidden = false;
            return this;
        }

        public Builder outsideScreenClick(Consumer<Click> outsideScreenClick) {
            this.options.outsideScreenClick = outsideScreenClick;
            return this;
        }

        public PageOptions build() {
            return this.options;
        }
    }

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

        public static FormFactor create(int slots, boolean hasRows, Function<PageOptions, MenuType<? extends AbstractContainerMenu>> menuTypeFunction) {
            return new FormFactor(slots, hasRows, menuTypeFunction);
        }

        public static void createDefaults(HolderLookup.Provider registryAccess) {
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
            // TODO: Mod proxy this.
            if (Mods.isPresent("pixelmon")) {
                FormFactor.COOKING_POT = FormFactor.create(11, false, options -> BuiltInRegistries.MENU.getOptional(COOKING_POT_ID).orElse(null));
                FormFactor.INFUSER = FormFactor.create(4, false, options -> BuiltInRegistries.MENU.getOptional(INFUSER_ID).orElse(null));
                FormFactor.WASHING_MACHINE = FormFactor.create(2, false, options -> BuiltInRegistries.MENU.getOptional(WASHING_MACHINE_ID).orElse(null));
                FormFactor.POKE_BAG = FormFactor.create(27, false, options -> BuiltInRegistries.MENU.getOptional(POKE_BAG_ID).orElse(null));
                FormFactor.DAY_CARE = FormFactor.create(0, false, options -> BuiltInRegistries.MENU.getOptional(DAY_CARE_ID).orElse(null));
            }
        }

        public MenuType<? extends AbstractContainerMenu> getType(PageOptions options) {
            return this.menuTypeFunction.apply(options);
        }
    }
}
