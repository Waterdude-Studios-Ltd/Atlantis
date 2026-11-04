package de.waterdu.atlantis.util.text;

import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link TextUtils}.
 */
@MinecraftBootstrapped
class TextUtilsTest {

    /**
     * Flattens a component and its siblings into a list of styled segments. Segments with no text are left out.
     *
     * @param component the component to flatten
     * @return a copy of each segment with its own style, in order
     */
    private static List<Component> segments(Component component) {
        var segments = new ArrayList<Component>();
        var own = component.plainCopy().withStyle(component.getStyle());
        if (!own.getString().isEmpty()) {
            segments.add(own);
        }
        for (var sibling : component.getSiblings()) {
            segments.addAll(segments(sibling));
        }
        return segments;
    }

    /**
     * Gets the RGB value of the color of a component. Throws a {@link NullPointerException} if the component has no
     * color.
     *
     * @param component the component to read
     * @return the RGB value
     */
    private static int colour(Component component) {
        return component.getStyle().getColor().getValue();
    }

    /**
     * Checks that {@link TextUtils#colorize} changes {@code &} codes to {@code §} codes and leaves plain and empty text
     * unchanged.
     */
    @Test
    void legacyColourCodesAreKept() {
        assertThat(TextUtils.colorize("&aGreen &lBold").getString()).isEqualTo("§aGreen §lBold");
        assertThat(TextUtils.colorize("plain").getString()).isEqualTo("plain");
        assertThat(TextUtils.colorize("").getString()).isEmpty();
    }

    /**
     * Checks that a {@code &#rrggbb} code is removed from the text and sets the color.
     */
    @Test
    void hexColour() {
        var component = TextUtils.colorize("&#ff0000Red");

        assertThat(component.getString()).isEqualTo("Red");
        assertThat(colour(component)).isEqualTo(0xff0000);
    }

    /**
     * Checks that text before a hex code stays uncolored and the text after it gets the color.
     */
    @Test
    void hexColourAfterPlainText() {
        var component = TextUtils.colorize("Plain &#00ff00Green");
        var segments = segments(component);

        assertThat(component.getString()).isEqualTo("Plain Green");
        assertThat(segments).hasSize(2);
        assertThat(segments.getFirst().getStyle().getColor()).isNull();
        assertThat(colour(segments.get(1))).isEqualTo(0x00ff00);
    }

    /**
     * Checks that each hex code starts a new segment with its own color, and that a legacy code after them is kept.
     */
    @Test
    void consecutiveHexColours() {
        var segments = segments(TextUtils.colorize("&#ff0000Red&#0000ffBlue&aLegacy"));

        assertThat(segments).extracting(Component::getString).containsExactly("Red", "Blue", "§aLegacy");
        assertThat(colour(segments.get(0))).isEqualTo(0xff0000);
        assertThat(colour(segments.get(1))).isEqualTo(0x0000ff);
    }

    /**
     * Checks that style codes in brackets after a hex code apply only the given styles.
     */
    @Test
    void hexStyles() {
        var component = TextUtils.colorize("&#ff0000[lo]Styled");

        assertThat(component.getString()).isEqualTo("Styled");
        assertThat(component.getStyle().isBold()).isTrue();
        assertThat(component.getStyle().isItalic()).isTrue();
        assertThat(component.getStyle().isUnderlined()).isFalse();
    }

    /**
     * Checks that a {@code #} that is not part of a hex code stays in the text.
     */
    @Test
    void leadingHashIsKept() {
        assertThat(TextUtils.colorize("#1 Player").getString()).isEqualTo("#1 Player");
        assertThat(TextUtils.colorize("#1 &#ff0000Player").getString()).isEqualTo("#1 Player");
    }

    /**
     * Checks that a gradient code gives one segment for each character, from the start color to the end color.
     */
    @Test
    void gradientRunsFromStartToEndColour() {
        var component = TextUtils.colorize("&#@ff0000-0000ffABCD");
        var segments = segments(component);

        assertThat(component.getString()).isEqualTo("ABCD");
        assertThat(segments).hasSize(4);
        assertThat(colour(segments.getFirst())).isEqualTo(0xff0000);
        assertThat(colour(segments.getLast())).isEqualTo(0x0000ff);
    }

    /**
     * Checks that style codes after a gradient code apply to each segment of the gradient.
     */
    @Test
    void gradientWithStyles() {
        var segments = segments(TextUtils.colorize("&#@ff0000-0000ff[n]AB"));

        assertThat(segments).isNotEmpty().allSatisfy(segment -> assertThat(segment.getStyle().isUnderlined()).isTrue());
    }

    /**
     * Checks that {@link TextUtils#getStyleArray} sets a flag for each style code in any case, and ignores other
     * codes.
     *
     * @param codes the style codes
     * @param obfuscated the expected obfuscated flag
     * @param bold the expected bold flag
     * @param strikethrough the expected strikethrough flag
     * @param underlined the expected underlined flag
     * @param italic the expected italic flag
     */
    @ParameterizedTest
    @CsvSource({
            "klmno, true,  true,  true,  true,  true",
            "LO,    false, true,  false, false, true",
            "lr,    false, false, false, false, false",
            "x,     false, false, false, false, false",
    })
    void styleArray(String codes, boolean obfuscated, boolean bold, boolean strikethrough, boolean underlined, boolean italic) {
        assertThat(TextUtils.getStyleArray(codes)).containsExactly(obfuscated, bold, strikethrough, underlined, italic);
    }

    /**
     * Checks that {@link TextUtils#applyStyleArray} applies the styles to the component and to its siblings.
     */
    @Test
    void applyStyleArrayReachesSiblings() {
        var component = Component.literal("a").append(Component.literal("b"));

        TextUtils.applyStyleArray(component, new boolean[]{false, true, false, false, false});

        assertThat(component.getStyle().isBold()).isTrue();
        assertThat(component.getSiblings().getFirst().getStyle().isBold()).isTrue();
    }

    /**
     * Checks that {@link TextUtils#format} replaces numbered placeholders, including repeated and two-digit ones.
     */
    @Test
    void format() {
        assertThat(TextUtils.format("%1 %2 %1", "a", Component.literal("b")).get()).isEqualTo("a b a");
        assertThat(TextUtils.format(Text.of("%1 of %11"), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).get()).isEqualTo("1 of 11");
        assertThat(TextUtils.format("no placeholders").get()).isEqualTo("no placeholders");
    }

    /**
     * Checks that {@link TextUtils#combine} joins strings, components and other objects with no separator.
     */
    @Test
    void combine() {
        assertThat(TextUtils.combine("a", Component.literal("b"), 3).getString()).isEqualTo("ab3");
    }

    /**
     * Checks the start index, end index and separator of the {@code concat} methods, and the {@code concatExcept} and
     * {@code join} methods.
     */
    @Test
    void concat() {
        var words = new String[]{"a", "b", "c", "d"};

        assertThat(TextUtils.concat(words)).isEqualTo("a b c d");
        assertThat(TextUtils.concat(1, ",", words)).isEqualTo("b,c,d");
        assertThat(TextUtils.concat(1, 3, "-", words)).isEqualTo("b-c");
        assertThat(TextUtils.concat(5, " ", words)).isEmpty();
        assertThat(TextUtils.concatExcept(2, "X", " ", words)).isEqualTo("a b X d");
        assertThat(TextUtils.join(new String[]{"a"}, "b", "c")).containsExactly("a", "b", "c");
    }

    /**
     * Checks that {@code stripNewlines} removes only real newlines, and that {@code stripSpecialCharacters} removes
     * accented characters but keeps spaces.
     */
    @Test
    void stripping() {
        assertThat(TextUtils.stripNewlines("a\nb\\nc")).isEqualTo("ab\\nc");
        assertThat(TextUtils.stripSpecialCharacters("héllo wörld")).isEqualTo("hllo wrld");
    }

    /**
     * Checks that {@link TextUtils#capitalizeEachWord} capitalizes each word except the given minor words, and always
     * capitalizes the first word.
     */
    @Test
    void capitalizeEachWord() {
        assertThat(TextUtils.capitalizeEachWord("THE lord OF the rings")).isEqualTo("The Lord of the Rings");
        assertThat(TextUtils.capitalizeEachWord("a tale", Set.of())).isEqualTo("A Tale");
        assertThat(TextUtils.capitalizeEachWord("tale of a tale", Set.of("tale"))).isEqualTo("Tale Of A tale");
        assertThat(TextUtils.capitalizeEachWord(Text.of("of mice")).get()).isEqualTo("Of Mice");
    }

    /**
     * Checks that {@code asText}, {@code asComponent}, {@code asString}, {@code toArray} and {@code toMutableArray}
     * convert each kind of object, and return text and components unchanged where possible.
     */
    @Test
    void textConversions() {
        var text = Text.of("text");

        assertThat((Object) TextUtils.asText(text)).isSameAs(text);
        assertThat(TextUtils.asText(Component.literal("component")).get()).isEqualTo("component");
        assertThat(TextUtils.asText(null).get()).isEmpty();
        assertThat(TextUtils.asText(42).get()).isEqualTo("42");

        MutableComponent component = Component.literal("c");
        assertThat(TextUtils.asComponent(component)).isSameAs(component);
        assertThat(TextUtils.asComponent(Text.of("&aX")).getString()).isEqualTo("§aX");
        assertThat(TextUtils.asComponent(7).getString()).isEqualTo("7");

        assertThat(TextUtils.asString(component)).isEqualTo("c");
        assertThat(TextUtils.asString(text)).isEqualTo("text");
        assertThat(TextUtils.asString(7)).isEqualTo("7");

        var array = TextUtils.toArray(new Object[]{"a", Text.Immutable.of("b")});
        assertThat(array).extracting(Text::get).containsExactly("a", "b");
        assertThat((Object) TextUtils.toMutableArray(new Object[]{Text.Immutable.of("b")})[0]).isNotInstanceOf(Text.Immutable.class);
    }

    /**
     * Checks that with no placeholder provider, the placeholder methods leave the placeholders in the text.
     */
    @Test
    void placeholdersWithoutAProviderLeaveTextUnchanged() {
        assertThat(TextUtils.asTextWithPlaceholders("%player%", new Object()).get()).isEqualTo("%player%");
        assertThat(TextUtils.asTextWithPlaceholders("%player%", List.<Object>of("context")).get()).isEqualTo("%player%");
        assertThat(TextUtils.asComponentWithPlaceholders("&aHi", new Object()).getString()).isEqualTo("§aHi");
        assertThat(Text.of("%x%").formatPlaceholders(1, 2).get()).isEqualTo("%x%");
    }

    /**
     * Checks that {@code countCharOccurrence} counts a character, and that {@code countCharOccurrenceDifference} gives
     * the difference between the counts of two characters.
     */
    @Test
    void counting() {
        assertThat(TextUtils.countCharOccurrence("a(b(c))", '(')).isEqualTo(2);
        assertThat(TextUtils.countCharOccurrenceDifference("a(b(c)", '(', ')')).isEqualTo(1);
    }

    /**
     * Checks that {@code isNumber} accepts only digits, that {@code isNumberParsable} accepts any parsable number and
     * that {@code isNumeral} accepts only a digit.
     */
    @Test
    void numbers() {
        assertThat(TextUtils.isNumber("0123")).isTrue();
        assertThat(TextUtils.isNumber("1.5")).isFalse();
        assertThat(TextUtils.isNumber("-1")).isFalse();
        assertThat(TextUtils.isNumberParsable("-1.5e3")).isTrue();
        assertThat(TextUtils.isNumberParsable("abc")).isFalse();
        assertThat(TextUtils.isNumeral('7')).isTrue();
        assertThat(TextUtils.isNumeral('x')).isFalse();
    }

    /**
     * Checks that {@link TextUtils#empty()} gives a component that holds one space.
     */
    @Test
    void emptyIsASpace() {
        assertThat(TextUtils.empty().getString()).isEqualTo(" ");
    }
}
