package de.waterdu.atlantis.util.text;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MinecraftBootstrapped
class TextTest {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Text.class, new Text.TypeAdapter())
            .registerTypeAdapter(Text.Immutable.class, new Text.Immutable.TypeAdapter())
            .create();

    @Test
    void factories() {
        assertThat(Text.of("value").get()).isEqualTo("value");
        assertThat(Text.of(Component.literal("component")).get()).isEqualTo("component");
        assertThat(Text.of((Object) 5).get()).isEqualTo("5");
        assertThat(Text.of((Object) null).get()).isEmpty();
        assertThat((Object) Text.empty()).isInstanceOf(Text.Immutable.class);
        assertThat(Text.empty().isEmpty()).isTrue();
        assertThat((Object) Text.of(Text.of("mutable"))).isInstanceOf(Text.Immutable.class).isEqualTo(Text.of("mutable"));
    }

    @Test
    void mutableTextChangesInPlace() {
        var text = Text.of("b");

        assertThat((Object) text.append("c").prepend("a")).isSameAs(text);
        assertThat(text.get()).isEqualTo("abc");
        assertThat(text.append(Text.of("d")).prepend(Text.of("_")).get()).isEqualTo("_abcd");
        assertThat(text.set("x").get()).isEqualTo("x");
        assertThat(text.set(Text.of("y")).get()).isEqualTo("y");
        assertThat(text.clear().isEmpty()).isTrue();
    }

    @Test
    void immutableTextReturnsCopies() {
        var text = Text.Immutable.of("b");

        assertThat(text.append("c").get()).isEqualTo("bc");
        assertThat(text.prepend("a").get()).isEqualTo("ab");
        assertThat(text.append(Text.of("c")).get()).isEqualTo("bc");
        assertThat(text.prepend(Text.of("a")).get()).isEqualTo("ab");
        assertThat(text.set("x").get()).isEqualTo("x");
        assertThat(text.set(Text.of("x")).get()).isEqualTo("x");
        assertThat(text.clear().isEmpty()).isTrue();
        assertThat(text.get()).isEqualTo("b");
    }

    @Test
    void immutableFlagsReturnCopies() {
        var text = Text.Immutable.of("&aGreen");

        var raw = text.raw();

        assertThat((Object) raw).isNotSameAs(text);
        assertThat(raw.compile().getString()).isEqualTo("&aGreen");
        assertThat(text.compile().getString()).isEqualTo("§aGreen");
        assertThat(raw.compiled().compile().getString()).isEqualTo("§aGreen");
        assertThat(text.translatable().compile().getContents()).isInstanceOf(TranslatableContents.class);
        assertThat(text.compile().getContents()).isNotInstanceOf(TranslatableContents.class);
    }

    @Test
    void mutableAndImmutableConversions() {
        var immutable = Text.Immutable.of("value");

        assertThat((Object) immutable.mutable()).isNotInstanceOf(Text.Immutable.class).isEqualTo(immutable);
        assertThat((Object) Text.of("value").immutable()).isInstanceOf(Text.Immutable.class);
        assertThat((Object) Text.of("value").copy()).isEqualTo(Text.of("value")).isNotSameAs(immutable);
    }

    @Test
    void stringOperations() {
        var text = Text.of("Hello World");

        assertThat(text.contains("lo W")).isTrue();
        assertThat(text.startsWith("Hello")).isTrue();
        assertThat(text.startsWith(Text.of("World"), 6)).isTrue();
        assertThat(text.startsWith("World", 6)).isTrue();
        assertThat(text.startsWith(Text.of("Hello"))).isTrue();
        assertThat(text.endsWith("World")).isTrue();
        assertThat(text.endsWith(Text.of("World"))).isTrue();
        assertThat(text.equalsIgnoreCase("hello world")).isTrue();
        assertThat(text.equalsIgnoreCase(Text.of("HELLO WORLD"))).isTrue();
        assertThat(text.split(" ")).containsExactly("Hello", "World");
        assertThat(text.split(Text.of("o"))).containsExactly("Hell", " W", "rld");
        assertThat(text.replace("World", "There").get()).isEqualTo("Hello There");
        assertThat(text.toLowerCase().get()).isEqualTo("hello world");
        assertThat(text.toUpperCase().get()).isEqualTo("HELLO WORLD");
        assertThat(Text.of("  padded ").trim().get()).isEqualTo("padded");
        assertThat(text.get()).isEqualTo("Hello World");
    }

    @Test
    void charSequenceAndComparison() {
        var text = Text.of("abc");

        assertThat(text.length()).isEqualTo(3);
        assertThat(text.charAt(1)).isEqualTo('b');
        assertThat(text.subSequence(1, 3).toString()).isEqualTo("bc");
        assertThat(text.chars().toArray()).containsExactly('a', 'b', 'c');
        assertThat(text.codePoints().count()).isEqualTo(3);
        assertThat(text.toString()).isEqualTo("abc");
        assertThat(text.compareTo(Text.of("abd"))).isNegative();
        assertThat((Object) text).isEqualTo(Text.of("abc")).hasSameHashCodeAs("abc");
        assertThat(text.equals("abc")).isTrue();
    }

    @Test
    void format() {
        assertThat(Text.of("%1 has %2 points, %10 total").format("Steve", 5, 1, 2, 3, 4, 5, 6, 7, 8).get())
                .isEqualTo("Steve has 5 points, 8 total");
    }

    @Test
    void compileEmptyTextAddsASpaceUnlessAsked() {
        assertThat(Text.of("").compile().getString()).isEqualTo(" ");
        assertThat(Text.of("").compile(false).getString()).isEmpty();
    }

    @Test
    void compileWithEventsAppliesToPlainText() {
        var click = Text.of("click").compileWithEvent(ClickEvent.Action.RUN_COMMAND, "/atlantis");
        var hover = Text.of("hover").compileWithEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("tip"));
        var both = Text.of("both").compileWithEvents(ClickEvent.Action.SUGGEST_COMMAND, "/x", HoverEvent.Action.SHOW_TEXT, Component.literal("tip"));

        assertThat(click.getStyle().getClickEvent()).isEqualTo(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/atlantis"));
        assertThat(hover.getStyle().getHoverEvent()).isEqualTo(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("tip")));
        assertThat(both.getStyle().getClickEvent()).isEqualTo(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/x"));
        assertThat(both.getStyle().getHoverEvent()).isNotNull();
    }

    @Test
    void compileWithEventsAppliesToEverySegment() {
        var component = Text.of("&#ff0000Red &#00ff00Green").compileWithEvent(ClickEvent.Action.RUN_COMMAND, "/x");

        assertThat(component.getStyle().getClickEvent()).isNotNull();
        for (var sibling : component.getSiblings()) {
            assertThat(sibling.getStyle().getClickEvent()).isEqualTo(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/x"));
        }
    }

    @Test
    void typeAdapters() {
        assertThat(this.gson.toJson(Text.of("value"), Text.class)).isEqualTo("\"value\"");
        assertThat(this.gson.fromJson("\"value\"", Text.class).get()).isEqualTo("value");

        assertThat(this.gson.toJson(Text.Immutable.of("value"), Text.Immutable.class)).isEqualTo("\"value\"");
        var flagged = (Text.Immutable) Text.Immutable.of("value");
        flagged.raw = true;
        var json = this.gson.toJson(flagged, Text.Immutable.class);
        assertThat(json).isEqualTo("{\"value\":\"value\",\"raw\":true,\"translate\":false}");
        var parsed = this.gson.fromJson(json, Text.Immutable.class);
        assertThat(parsed.raw).isTrue();
        assertThat(parsed.translate).isFalse();
        assertThat(this.gson.fromJson("\"plain\"", Text.Immutable.class).raw).isFalse();
    }
}
