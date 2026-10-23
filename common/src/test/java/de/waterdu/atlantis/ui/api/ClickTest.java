package de.waterdu.atlantis.ui.api;

import net.minecraft.world.inventory.ClickType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClickTest {

    @ParameterizedTest
    @CsvSource({"0, LEFT", "1, RIGHT", "2, MIDDLE", "3, UNKNOWN", "-1, UNKNOWN", "40, UNKNOWN"})
    void buttonFromMouseButton(int mouseButton, Click.Button expected) {
        assertThat(Click.Button.get(mouseButton)).isEqualTo(expected);
    }

    @Test
    void buttonPredicates() {
        assertThat(Click.Button.LEFT.isLeft()).isTrue();
        assertThat(Click.Button.RIGHT.isRight()).isTrue();
        assertThat(Click.Button.MIDDLE.isMiddle()).isTrue();
        assertThat(Click.Button.UNKNOWN.isUnknown()).isTrue();
        assertThat(Click.Button.LEFT.isRight()).isFalse();
        assertThat(Click.Button.RIGHT.isMiddle()).isFalse();
        assertThat(Click.Button.MIDDLE.isUnknown()).isFalse();
        assertThat(Click.Button.UNKNOWN.isLeft()).isFalse();
    }

    @Test
    void buttonValuesAndNullEndsWithNull() {
        assertThat(Click.Button.valuesAndNull()).containsExactly(Click.Button.LEFT, Click.Button.RIGHT, Click.Button.MIDDLE, Click.Button.UNKNOWN, null);
    }

    @ParameterizedTest
    @CsvSource({
            "PICKUP, NORMAL",
            "CLONE, NORMAL",
            "QUICK_MOVE, SHIFT",
            "QUICK_CRAFT, DRAG",
            "SWAP, UNKNOWN",
            "THROW, UNKNOWN",
            "PICKUP_ALL, UNKNOWN",
    })
    void styleFromClickType(ClickType type, Click.Style expected) {
        assertThat(Click.Style.get(type)).isEqualTo(expected);
    }

    @Test
    void styleValuesAndNullEndsWithNull() {
        assertThat(Click.Style.valuesAndNull()).containsExactly(Click.Style.NORMAL, Click.Style.SHIFT, Click.Style.DRAG, Click.Style.UNKNOWN, null);
    }
}
