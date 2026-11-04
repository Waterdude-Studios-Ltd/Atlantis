package de.waterdu.atlantis.util.server;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link CommandUtils}.
 */
class CommandUtilsTest {

    /**
     * Checks that {@link CommandUtils#doesStringStartWith} ignores case and that an empty prefix matches any string.
     */
    @Test
    void doesStringStartWithIgnoresCase() {
        assertThat(CommandUtils.doesStringStartWith("re", "Reload")).isTrue();
        assertThat(CommandUtils.doesStringStartWith("", "anything")).isTrue();
        assertThat(CommandUtils.doesStringStartWith("reload", "re")).isFalse();
        assertThat(CommandUtils.doesStringStartWith("x", "reload")).isFalse();
    }

    /**
     * Checks that {@link CommandUtils#getListOfStringsMatchingLastWord} keeps the completions that start with the
     * last argument.
     */
    @Test
    void matchesTheLastArgument() {
        var args = new String[]{"atlantis", "re"};

        assertThat(CommandUtils.getListOfStringsMatchingLastWord(args, "reload", "rename", "mods")).containsExactly("reload", "rename");
        assertThat(CommandUtils.getListOfStringsMatchingLastWord(new String[]{""}, "a", "b")).containsExactly("a", "b");
        assertThat(CommandUtils.getListOfStringsMatchingLastWord(args, List.of())).isEmpty();
    }

    /**
     * Checks that completions that are not strings are matched and returned by their {@code toString} value.
     */
    @Test
    void usesToStringOfNonStringCompletions() {
        assertThat(CommandUtils.getListOfStringsMatchingLastWord(new String[]{"1"}, List.of(10, 20, 15))).containsExactly("10", "15");
    }

    /**
     * Checks that a resource location matches by its full name or by its path, and is returned by its full name.
     */
    @Test
    void fallsBackToResourceLocationPaths() {
        var stone = ResourceLocation.withDefaultNamespace("stone");
        var stick = ResourceLocation.fromNamespaceAndPath("atlantis", "stick");
        var dirt = ResourceLocation.withDefaultNamespace("dirt");

        assertThat(CommandUtils.getListOfStringsMatchingLastWord(new String[]{"st"}, List.of(stone, stick, dirt)))
                .containsExactly("minecraft:stone", "atlantis:stick");
        assertThat(CommandUtils.getListOfStringsMatchingLastWord(new String[]{"atlantis:"}, List.of(stone, stick)))
                .containsExactly("atlantis:stick");
    }
}
