package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.world.inventory.MenuType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
class PageOptionsTest {

    @Test
    void defaultsHaveAFormFactorBeforeCreateDefaults() {
        assertThat(PageOptions.defaults().formFactor()).isNotNull().isSameAs(PageOptions.FormFactor.CHEST);
        assertThat(PageOptions.builder().build().formFactor()).isSameAs(PageOptions.FormFactor.CHEST);
    }

    @Test
    void vanillaFormFactorsExistBeforeCreateDefaults() {
        var options = PageOptions.defaults();
        assertThat(PageOptions.FormFactor.FURNACE.getType(options)).isSameAs(MenuType.FURNACE);
        assertThat(PageOptions.FormFactor.HOPPER.getType(options)).isSameAs(MenuType.HOPPER);
        assertThat(PageOptions.FormFactor.STONECUTTER.getType(options)).isSameAs(MenuType.STONECUTTER);
        assertThat(PageOptions.FormFactor.DISPENSER.slots()).isEqualTo(9);
        assertThat(PageOptions.FormFactor.DISPENSER.hasRows()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"1, 9x1", "2, 9x2", "3, 9x3", "4, 9x4", "5, 9x5", "6, 9x6", "0, 9x3", "7, 9x3"})
    void chestMenuTypeFollowsTheRows(int rows, String size) {
        var options = PageOptions.builder().rows(rows).build();
        var expected = switch (size) {
            case "9x1" -> MenuType.GENERIC_9x1;
            case "9x2" -> MenuType.GENERIC_9x2;
            case "9x4" -> MenuType.GENERIC_9x4;
            case "9x5" -> MenuType.GENERIC_9x5;
            case "9x6" -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };
        assertThat(PageOptions.FormFactor.CHEST.getType(options)).isSameAs(expected);
    }

    @Test
    void builderSetsEveryOption() {
        var options = PageOptions.builder()
                .formFactor(PageOptions.FormFactor.HOPPER)
                .rows(5)
                .title("Title")
                .refreshRate(20)
                .hideInventory()
                .build();

        assertThat(options.formFactor()).isSameAs(PageOptions.FormFactor.HOPPER);
        assertThat(options.rows()).isEqualTo(5);
        assertThat(options.title().compile().getString()).isEqualTo("Title");
        assertThat(options.refreshRate()).isEqualTo(20);
        assertThat(options.isInventoryHidden()).isTrue();
    }

    @Test
    void aDraggablePageNeverHidesTheInventory() {
        var options = PageOptions.builder().hideInventory().draggable(true).build();

        assertThat(options.isDraggable()).isTrue();
        assertThat(options.isInventoryHidden()).isFalse();
    }
}
