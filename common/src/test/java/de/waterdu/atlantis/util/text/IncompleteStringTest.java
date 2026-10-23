package de.waterdu.atlantis.util.text;

import de.waterdu.atlantis.test.MinecraftBootstrapped;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@MinecraftBootstrapped
class IncompleteStringTest {

    private static IncompleteString greeting() {
        return IncompleteString.builder().string("Hello ").placeholder().string(", you have ").placeholder().string(" coins").build();
    }

    @Test
    void completeFillsPlaceholdersInOrder() {
        assertThat(greeting().complete("Water", "5")).isEqualTo("Hello Water, you have 5 coins");
        assertThat(greeting().complete((Object) "Water", 5)).isEqualTo("Hello Water, you have 5 coins");
    }

    @Test
    void missingPlaceholdersAreLeftOut() {
        assertThat(greeting().complete("Water")).isEqualTo("Hello Water, you have  coins");
    }

    @Test
    void wrapperIgnoresPlaceholders() {
        assertThat(IncompleteString.of("plain").complete("ignored")).isEqualTo("plain");
    }

    @Test
    void singleStringBuildsAWrapper() {
        assertThat(IncompleteString.builder().string("only").build().complete()).isEqualTo("only");
        assertThat(IncompleteString.builder().placeholder().build().complete("filled")).isEqualTo("filled");
    }

    @Test
    void appendJoinsTheLastString() {
        var string = IncompleteString.builder().append("a").append('b').placeholder().append("c").append("d").build();

        assertThat(string.complete("_")).isEqualTo("ab_cd");
    }

    @Test
    void builderRejectsNullStrings() {
        assertThatNullPointerException().isThrownBy(() -> IncompleteString.builder().string(null));
        assertThat(IncompleteString.builder().isEmpty()).isTrue();
    }

    @Test
    void replaceTextModifiesInPlace() {
        var string = greeting();

        var result = string.replace("coins", "gems");

        assertThat(result).isSameAs(string);
        assertThat(string.complete("Water", "5")).isEqualTo("Hello Water, you have 5 gems");
    }

    @Test
    void replaceWithAnIncompleteStringSplicesItIn() {
        var query = IncompleteString.builder().string("SELECT * FROM %t WHERE id = ").placeholder().build();
        var table = IncompleteString.builder().string("db.").placeholder().build();

        var result = query.replace("%t", table);

        assertThat(result.complete("users", "7")).isEqualTo("SELECT * FROM db.users WHERE id = 7");
        assertThat(query.complete("7")).isEqualTo("SELECT * FROM %t WHERE id = 7");
    }

    @Test
    void replaceOnAWrapper() {
        var result = IncompleteString.of("a %x b").replace("%x", IncompleteString.builder().placeholder().build());

        assertThat(result.complete("X")).isEqualTo("a X b");
        assertThat(IncompleteString.of("no target").replace("%x", IncompleteString.of("y")).complete()).isEqualTo("no target");
    }

    @Test
    void copyIsIndependent() {
        var original = greeting();
        var copy = original.copy();
        copy.replace("Hello", "Bye");
        var wrapper = IncompleteString.of("w");
        wrapper.copy().replace("w", "changed");

        assertThat(original.complete("a", "b")).startsWith("Hello");
        assertThat(copy.complete("a", "b")).startsWith("Bye");
        assertThat(wrapper.complete()).isEqualTo("w");
    }

    @Test
    void mergeCopiesPartsAndPlaceholders() {
        var merged = IncompleteString.builder().string("[").merge(greeting()).string("]").merge(IncompleteString.of("!")).build();

        assertThat(merged.complete("Water", "5")).isEqualTo("[Hello Water, you have 5 coins]!");
    }

    @Test
    void completeAsTextAndComponent() {
        assertThat(greeting().completeAsText("Water", 5).toString()).isEqualTo("Hello Water, you have 5 coins");
        assertThat(greeting().completeAsComponent("Water", 5).getString()).isEqualTo("Hello Water, you have 5 coins");
        assertThat(IncompleteString.of("plain").completeAsComponent().getString()).isEqualTo("plain");
        assertThat(IncompleteString.of("plain").completeAsText().toString()).isEqualTo("plain");
    }
}
