package de.waterdu.atlantis.util.server;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandUtilsTest {

    @Test
    void doesStringStartWithIgnoresCase() {
        assertThat(CommandUtils.doesStringStartWith("re", "Reload")).isTrue();
        assertThat(CommandUtils.doesStringStartWith("", "anything")).isTrue();
        assertThat(CommandUtils.doesStringStartWith("reload", "re")).isFalse();
        assertThat(CommandUtils.doesStringStartWith("x", "reload")).isFalse();
    }

    @Test
    void matchesTheLastArgument() {
        var args = new String[]{"atlantis", "re"};

        assertThat(CommandUtils.getListOfStringsMatchingLastWord(args, "reload", "rename", "mods")).containsExactly("reload", "rename");
        assertThat(CommandUtils.getListOfStringsMatchingLastWord(new String[]{""}, "a", "b")).containsExactly("a", "b");
        assertThat(CommandUtils.getListOfStringsMatchingLastWord(args, List.of())).isEmpty();
    }

    @Test
    void usesToStringOfNonStringCompletions() {
        assertThat(CommandUtils.getListOfStringsMatchingLastWord(new String[]{"1"}, List.of(10, 20, 15))).containsExactly("10", "15");
    }

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
