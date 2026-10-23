package de.waterdu.atlantis.ui.internal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FontUtilsTest {

    @BeforeAll
    static void init() throws IOException {
        FontUtils.init();
    }

    @Test
    void initLoadsTheBundledWidths() {
        assertThatCode(FontUtils::init).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "a, 6",
            "i, 2",
            "l, 3",
            "W, 6",
            "@, 7",
            "!, 2",
            "., 2",
            "é, 6",
    })
    void charWidthsMatchVanilla(char character, int width) {
        assertThat(FontUtils.getCharWidth(character)).isEqualTo(width);
    }

    @Test
    void spacesAndFormattingCodes() {
        assertThat(FontUtils.getCharWidth(' ')).isEqualTo(4);
        assertThat(FontUtils.getCharWidth(' ')).isEqualTo(4);
        assertThat(FontUtils.getCharWidth('§')).isEqualTo(-1);
    }

    @Test
    void glyphTableCoversNonLatinCharacters() {
        assertThat(FontUtils.getCharWidth('Ж')).isPositive();
    }

    @Test
    void stringWidthSkipsFormattingCodesAndAddsBold() {
        assertThat(FontUtils.getStringWidth("ai")).isEqualTo(8);
        assertThat(FontUtils.getStringWidth("§cai")).isEqualTo(8);
        assertThat(FontUtils.getStringWidth("§lai§ra")).isEqualTo(16);
        assertThat(FontUtils.getStringWidth(null)).isZero();
    }
}
