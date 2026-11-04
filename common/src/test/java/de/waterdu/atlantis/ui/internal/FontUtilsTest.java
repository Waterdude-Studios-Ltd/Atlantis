package de.waterdu.atlantis.ui.internal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests the character and string widths that {@link FontUtils} reports.
 */
class FontUtilsTest {

    /**
     * Loads the font widths once before the tests.
     */
    @BeforeAll
    static void init() throws IOException {
        FontUtils.init();
    }

    /**
     * Checks that {@link FontUtils#init()} loads the bundled widths without an exception.
     */
    @Test
    void initLoadsTheBundledWidths() {
        assertThatCode(FontUtils::init).doesNotThrowAnyException();
    }

    /**
     * Checks that character widths match the vanilla font.
     *
     * @param character the character to measure
     * @param width the expected width in pixels
     */
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

    /**
     * Checks that spaces are 4 pixels wide and that the formatting code character reports {@code -1}.
     */
    @Test
    void spacesAndFormattingCodes() {
        assertThat(FontUtils.getCharWidth(' ')).isEqualTo(4);
        assertThat(FontUtils.getCharWidth(' ')).isEqualTo(4);
        assertThat(FontUtils.getCharWidth('§')).isEqualTo(-1);
    }

    /**
     * Checks that a non-Latin character has a positive width.
     */
    @Test
    void glyphTableCoversNonLatinCharacters() {
        assertThat(FontUtils.getCharWidth('Ж')).isPositive();
    }

    /**
     * Checks that string widths skip formatting codes, add one pixel for each bold character, and are {@code 0} for
     * {@code null}.
     */
    @Test
    void stringWidthSkipsFormattingCodesAndAddsBold() {
        assertThat(FontUtils.getStringWidth("ai")).isEqualTo(8);
        assertThat(FontUtils.getStringWidth("§cai")).isEqualTo(8);
        assertThat(FontUtils.getStringWidth("§lai§ra")).isEqualTo(16);
        assertThat(FontUtils.getStringWidth(null)).isZero();
    }
}
