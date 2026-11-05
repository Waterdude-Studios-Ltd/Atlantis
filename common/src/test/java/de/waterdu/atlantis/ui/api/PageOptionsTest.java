package de.waterdu.atlantis.ui.api;

import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.world.inventory.MenuType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests {@link PageOptions}, its builder and its form factors.
 */
@MinecraftBootstrapped
class PageOptionsTest {

    /**
     * Checks that the default options and an empty builder use {@link PageOptions.FormFactor#CHEST}.
     */
    @Test
    void defaultsHaveAFormFactorBeforeCreateDefaults() {
        assertThat(PageOptions.defaults().formFactor()).isNotNull().isSameAs(PageOptions.FormFactor.CHEST);
        assertThat(PageOptions.builder().build().formFactor()).isSameAs(PageOptions.FormFactor.CHEST);
    }

    /**
     * Checks that the vanilla form factors have the correct menu types and slots.
     */
    @Test
    void vanillaFormFactorsExistBeforeCreateDefaults() {
        var options = PageOptions.defaults();
        assertThat(PageOptions.FormFactor.FURNACE.getType(options)).isSameAs(MenuType.FURNACE);
        assertThat(PageOptions.FormFactor.HOPPER.getType(options)).isSameAs(MenuType.HOPPER);
        assertThat(PageOptions.FormFactor.STONECUTTER.getType(options)).isSameAs(MenuType.STONECUTTER);
        assertThat(PageOptions.FormFactor.DISPENSER.slots()).isEqualTo(9);
        assertThat(PageOptions.FormFactor.DISPENSER.hasRows()).isFalse();
    }

    /**
     * Checks that the chest menu type matches the number of rows, and that a row count out of range gives 9x3.
     *
     * @param rows the number of rows
     * @param size the expected menu size, such as {@code 9x3}
     */
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

    /**
     * Checks that the builder sets the form factor, rows, title, refresh rate and hidden inventory.
     */
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

    /**
     * Checks that a draggable page does not hide the inventory, even if the builder asks it to.
     */
    @Test
    void aDraggablePageNeverHidesTheInventory() {
        var options = PageOptions.builder().hideInventory().draggable(true).build();

        assertThat(options.isDraggable()).isTrue();
        assertThat(options.isInventoryHidden()).isFalse();
    }

    /**
     * Checks that the hash code does not throw when the click handler has a negative hash code or the joined values
     * do not fit in a long.
     */
    @Test
    void hashCodeDoesNotThrowForAnyValues() {
        Consumer<Click> negative = new Consumer<>() {
            @Override
            public void accept(Click click) {}

            @Override
            public int hashCode() {
                return -42;
            }
        };
        var withNegativeHandler = PageOptions.builder().outsideScreenClick(negative).build();
        var withLargeRate = PageOptions.builder().refreshRate(Long.MAX_VALUE).title("A long title for the page").build();

        assertThatCode(withNegativeHandler::hashCode).doesNotThrowAnyException();
        assertThatCode(withLargeRate::hashCode).doesNotThrowAnyException();
    }

    /**
     * Checks that options with the same values are equal and have the same hash code.
     */
    @Test
    void optionsWithTheSameValuesAreEqual() {
        var first = PageOptions.builder().formFactor(PageOptions.FormFactor.HOPPER).title("Title").refreshRate(20).build();
        var second = PageOptions.builder().formFactor(PageOptions.FormFactor.HOPPER).title("Title").refreshRate(20).build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    /**
     * Checks that options are not equal when one value is different.
     */
    @Test
    void optionsWithADifferentValueAreNotEqual() {
        var base = PageOptions.builder().title("Title").build();

        assertThat(base).isNotEqualTo(PageOptions.builder().title("Other").build());
        assertThat(base).isNotEqualTo(PageOptions.builder().title("Title").rows(4).build());
        assertThat(base).isNotEqualTo(PageOptions.builder().title("Title").draggable(true).build());
        assertThat(base).isNotEqualTo(PageOptions.builder().title("Title").refreshRate(5).build());
        assertThat(base).isNotEqualTo(PageOptions.builder().title("Title").hideInventory().build());
        assertThat(base).isNotEqualTo(PageOptions.builder().title("Title").outsideScreenClick(click -> {}).build());
    }

    /**
     * Checks that two form factors with the same number of slots do not make the options equal.
     */
    @Test
    void formFactorsWithTheSameSlotsAreNotEqual() {
        assertThat(PageOptions.FormFactor.FURNACE.slots()).isEqualTo(PageOptions.FormFactor.SMOKER.slots());

        var furnace = PageOptions.builder().formFactor(PageOptions.FormFactor.FURNACE).build();
        var smoker = PageOptions.builder().formFactor(PageOptions.FormFactor.SMOKER).build();

        assertThat(furnace).isNotEqualTo(smoker);
    }

    /**
     * Checks that the hash code follows a change that the builder makes after {@code build}.
     */
    @Test
    void hashCodeFollowsLaterChanges() {
        var builder = PageOptions.builder();
        var options = builder.build();
        var before = options.hashCode();

        builder.rows(6);

        assertThat(options.hashCode()).isNotEqualTo(before).isEqualTo(PageOptions.builder().rows(6).build().hashCode());
    }
}
