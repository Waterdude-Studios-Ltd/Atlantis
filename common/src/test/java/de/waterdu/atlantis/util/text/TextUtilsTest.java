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

@MinecraftBootstrapped
class TextUtilsTest {

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

    private static int colour(Component component) {
        return component.getStyle().getColor().getValue();
    }

    @Test
    void legacyColourCodesAreKept() {
        assertThat(TextUtils.colorize("&aGreen &lBold").getString()).isEqualTo("§aGreen §lBold");
        assertThat(TextUtils.colorize("plain").getString()).isEqualTo("plain");
        assertThat(TextUtils.colorize("").getString()).isEmpty();
    }

    @Test
    void hexColour() {
        var component = TextUtils.colorize("&#ff0000Red");

        assertThat(component.getString()).isEqualTo("Red");
        assertThat(colour(component)).isEqualTo(0xff0000);
    }

    @Test
    void hexColourAfterPlainText() {
        var component = TextUtils.colorize("Plain &#00ff00Green");
        var segments = segments(component);

        assertThat(component.getString()).isEqualTo("Plain Green");
        assertThat(segments).hasSize(2);
        assertThat(segments.getFirst().getStyle().getColor()).isNull();
        assertThat(colour(segments.get(1))).isEqualTo(0x00ff00);
    }

    @Test
    void consecutiveHexColours() {
        var segments = segments(TextUtils.colorize("&#ff0000Red&#0000ffBlue&aLegacy"));

        assertThat(segments).extracting(Component::getString).containsExactly("Red", "Blue", "§aLegacy");
        assertThat(colour(segments.get(0))).isEqualTo(0xff0000);
        assertThat(colour(segments.get(1))).isEqualTo(0x0000ff);
    }

    @Test
    void hexStyles() {
        var component = TextUtils.colorize("&#ff0000[lo]Styled");

        assertThat(component.getString()).isEqualTo("Styled");
        assertThat(component.getStyle().isBold()).isTrue();
        assertThat(component.getStyle().isItalic()).isTrue();
        assertThat(component.getStyle().isUnderlined()).isFalse();
    }

    @Test
    void leadingHashIsKept() {
        assertThat(TextUtils.colorize("#1 Player").getString()).isEqualTo("#1 Player");
        assertThat(TextUtils.colorize("#1 &#ff0000Player").getString()).isEqualTo("#1 Player");
    }

    @Test
    void gradientRunsFromStartToEndColour() {
        var component = TextUtils.colorize("&#@ff0000-0000ffABCD");
        var segments = segments(component);

        assertThat(component.getString()).isEqualTo("ABCD");
        assertThat(segments).hasSize(4);
        assertThat(colour(segments.getFirst())).isEqualTo(0xff0000);
        assertThat(colour(segments.getLast())).isEqualTo(0x0000ff);
    }

    @Test
    void gradientWithStyles() {
        var segments = segments(TextUtils.colorize("&#@ff0000-0000ff[n]AB"));

        assertThat(segments).isNotEmpty().allSatisfy(segment -> assertThat(segment.getStyle().isUnderlined()).isTrue());
    }

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

    @Test
    void applyStyleArrayReachesSiblings() {
        var component = Component.literal("a").append(Component.literal("b"));

        TextUtils.applyStyleArray(component, new boolean[]{false, true, false, false, false});

        assertThat(component.getStyle().isBold()).isTrue();
        assertThat(component.getSiblings().getFirst().getStyle().isBold()).isTrue();
    }

    @Test
    void format() {
        assertThat(TextUtils.format("%1 %2 %1", "a", Component.literal("b")).get()).isEqualTo("a b a");
        assertThat(TextUtils.format(Text.of("%1 of %11"), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).get()).isEqualTo("1 of 11");
        assertThat(TextUtils.format("no placeholders").get()).isEqualTo("no placeholders");
    }

    @Test
    void combine() {
        assertThat(TextUtils.combine("a", Component.literal("b"), 3).getString()).isEqualTo("ab3");
    }

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

    @Test
    void stripping() {
        assertThat(TextUtils.stripNewlines("a\nb\\nc")).isEqualTo("ab\\nc");
        assertThat(TextUtils.stripSpecialCharacters("héllo wörld")).isEqualTo("hllo wrld");
    }

    @Test
    void capitalizeEachWord() {
        assertThat(TextUtils.capitalizeEachWord("THE lord OF the rings")).isEqualTo("The Lord of the Rings");
        assertThat(TextUtils.capitalizeEachWord("a tale", Set.of())).isEqualTo("A Tale");
        assertThat(TextUtils.capitalizeEachWord("tale of a tale", Set.of("tale"))).isEqualTo("Tale Of A tale");
        assertThat(TextUtils.capitalizeEachWord(Text.of("of mice")).get()).isEqualTo("Of Mice");
    }

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

    @Test
    void placeholdersWithoutAProviderLeaveTextUnchanged() {
        assertThat(TextUtils.asTextWithPlaceholders("%player%", new Object()).get()).isEqualTo("%player%");
        assertThat(TextUtils.asTextWithPlaceholders("%player%", List.<Object>of("context")).get()).isEqualTo("%player%");
        assertThat(TextUtils.asComponentWithPlaceholders("&aHi", new Object()).getString()).isEqualTo("§aHi");
        assertThat(Text.of("%x%").formatPlaceholders(1, 2).get()).isEqualTo("%x%");
    }

    @Test
    void counting() {
        assertThat(TextUtils.countCharOccurrence("a(b(c))", '(')).isEqualTo(2);
        assertThat(TextUtils.countCharOccurrenceDifference("a(b(c)", '(', ')')).isEqualTo(1);
    }

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

    @Test
    void emptyIsASpace() {
        assertThat(TextUtils.empty().getString()).isEqualTo(" ");
    }
}
