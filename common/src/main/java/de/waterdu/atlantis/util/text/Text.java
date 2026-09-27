package de.waterdu.atlantis.util.text;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.server.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NullMarked;

import java.awt.Color;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.IntStream;

@NullMarked
public class Text implements Serializable, Comparable<Text>, CharSequence {

    private String value;
    boolean raw = false;
    boolean translate = false;

    protected Text(String value) {
        this.value = value;
    }

    public static Text of(Object stringLike) {
        return TextUtils.asText(stringLike);
    }

    public static Text of(String string) {
        return new Text(string);
    }

    public static Text of(Component component) {
        return Text.of(component.getString());
    }

    public static Text of(Text text) {
        return text.immutable();
    }

    public static Text empty() {
        return new Immutable("");
    }

    public Text mutable() {
        return Text.of(this.get());
    }

    public Immutable immutable() {
        return Immutable.of(this.get());
    }

    public Text raw() {
        this.raw = true;
        return this;
    }

    public Text compiled() {
        this.raw = false;
        return this;
    }

    public Text translatable() {
        this.translate = true;
        return this;
    }

    public Text set(String string) {
        this.value = string;
        return this;
    }

    public Text clear() {
        this.value = "";
        return this;
    }

    public Text append(Object string) {
        this.value = this.value + string;
        return this;
    }

    public Text prepend(Object string) {
        this.value = string + this.value;
        return this;
    }

    public String[] split(String regex) {
        return this.value.split(regex);
    }

    public Text set(Text string) {
        this.value = string.value;
        return this;
    }

    public Text append(Text string) {
        this.value = this.value + string.value;
        return this;
    }

    public Text prepend(Text string) {
        this.value = string.value + this.value;
        return this;
    }

    public String[] split(Text regex) {
        return this.value.split(regex.value);
    }

    public boolean contains(CharSequence s) {
        return this.value.contains(s);
    }

    public boolean equalsIgnoreCase(String string) {
        return this.value.equalsIgnoreCase(string);
    }

    public boolean equalsIgnoreCase(Text string) {
        return this.value.equalsIgnoreCase(string.value);
    }

    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    public Text replace(CharSequence target, CharSequence replacement) {
        return Text.of(this.value.replace(target, replacement));
    }

    public Text toLowerCase() {
        return Text.of(this.value.toLowerCase());
    }

    public Text toUpperCase() {
        return Text.of(this.value.toUpperCase());
    }

    public Text trim() {
        return Text.of(this.value.trim());
    }

    public boolean startsWith(String prefix) {
        return this.value.startsWith(prefix);
    }

    public boolean startsWith(String prefix, int offset) {
        return this.value.startsWith(prefix, offset);
    }

    public boolean endsWith(String suffix) {
        return this.value.endsWith(suffix);
    }

    public boolean startsWith(Text prefix) {
        return this.value.startsWith(prefix.value);
    }

    public boolean startsWith(Text prefix, int offset) {
        return this.value.startsWith(prefix.value, offset);
    }

    public boolean endsWith(Text suffix) {
        return this.value.endsWith(suffix.value);
    }

    public String get() {
        return this.value;
    }

    public Text format(Object... args) {
        return TextUtils.format(this.value, args);
    }

    public Text formatPlaceholders(Object... contexts) {
        return TextUtils.asTextWithPlaceholders(this, contexts);
    }

    public Text formatPlaceholders(Collection<Object> contexts) {
        return TextUtils.asTextWithPlaceholders(this, contexts);
    }

    public void send(CommandSourceStack... receivers) {
        MessageUtils.send(this.compile(), receivers);
    }

    public void send(CommandSource... receivers) {
        MessageUtils.send(this.compile(), receivers);
    }

    public Command.CommandContext prepareCommand() {
        return Command.of(this).prepareContext();
    }

    public MutableComponent compile() {
        return this.compile(true);
    }

    public MutableComponent compile(boolean ifEmptyAddSpace) {
        String value = this.value;
        if (this.translate) {
            return Component.translatable(value);
        }
        if (ifEmptyAddSpace && this.value.isEmpty()) {
            value = " ";
        }
        if (this.raw) {
            return Component.literal(value);
        }
        return TextUtils.colorize(value);
    }

    public MutableComponent compileWithEvent(ClickEvent.Action action, String value) {
        return TextUtils.applyClickEvent(this.compile(), new ClickEvent(action, value));
    }

    public <T> MutableComponent compileWithEvent(HoverEvent.Action<T> action, T value) {
        return TextUtils.applyHoverEvent(this.compile(), new HoverEvent(action, value));
    }

    public <T> MutableComponent compileWithEvents(ClickEvent.Action clickEventAction, String clickEventValue,
                                                           HoverEvent.Action<T> hoverEventAction, T hoverEventValue) {
        return TextUtils.applyHoverEvent(TextUtils.applyClickEvent(this.compile(), new ClickEvent(clickEventAction, clickEventValue)), new HoverEvent(hoverEventAction, hoverEventValue));
    }

    public Text copy() {
        return Text.of(this.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Text) {
            return this.value.equals(((Text) obj).value);
        }
        return this.value.equals(obj);
    }

    @Override
    public int length() {
        return this.value.length();
    }

    @Override
    public char charAt(int index) {
        return this.value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return this.value.subSequence(start, end);
    }

    @Override
    public String toString() {
        return this.value;
    }

    @Override
    public IntStream chars() {
        return this.value.chars();
    }

    @Override
    public IntStream codePoints() {
        return this.value.codePoints();
    }

    @Override
    public int compareTo(Text text) {
        return this.value.compareTo(text.value);
    }

    public static class TypeAdapter implements JsonSerializer<Text>, JsonDeserializer<Text> {

        @Override
        public Text deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Text.of(json.getAsString());
        }

        @Override
        public JsonElement serialize(Text src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    @NullMarked
    public static class Immutable extends Text {

        protected Immutable(String value) {
            super(value);
        }

        public static Immutable of(String string) {
            return new Immutable(string);
        }

        public static Immutable of(Component component) {
            return Immutable.of(component.getString());
        }

        public static Immutable empty() {
            return new Immutable("");
        }

        @Override
        public Text raw() {
            var copy = this.copy();
            copy.raw = true;
            return copy;
        }

        @Override
        public Text compiled() {
            var copy = this.copy();
            copy.raw = false;
            return copy;
        }
        
        @Override
        public Text translatable() {
            var copy = this.copy();
            copy.translate = true;
            return copy;
        }

        @Override
        public Text set(String string) {
            return Immutable.of(string);
        }

        @Override
        public Text clear() {
            return Immutable.empty();
        }

        @Override
        public Text append(Object string) {
            return Immutable.of(this.get() + string);
        }

        @Override
        public Text prepend(Object string) {
            return Immutable.of(string + this.get());
        }

        public Text set(Text string) {
            return this.set(string.get());
        }

        public Text append(Text string) {
            return this.append(string.get());
        }

        public Text prepend(Text string) {
            return this.prepend(string.get());
        }

        public static class TypeAdapter implements JsonSerializer<Immutable>, JsonDeserializer<Immutable> {

            @Override
            public Immutable deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                if (json.isJsonPrimitive()) {
                    return Immutable.of(json.getAsString());
                } else {
                    var object = json.getAsJsonObject();
                    var text = Immutable.of(object.get("value").getAsString());
                    text.raw = object.has("raw") && object.get("raw").getAsBoolean();
                    text.translate = object.has("translate") && object.get("translate").getAsBoolean();
                    return text;
                }
            }

            @Override
            public JsonElement serialize(Immutable src, Type typeOfSrc, JsonSerializationContext context) {
                if (!src.raw && !src.translate) {
                    return new JsonPrimitive(src.toString());
                } else {
                    var object = new JsonObject();
                    object.addProperty("value", src.get());
                    object.addProperty("raw", src.raw);
                    object.addProperty("translate", src.translate);
                    return object;
                }
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @NullMarked
    public static class Builder {

        private final StringBuilder text;

        public Builder() {
            this.text = new StringBuilder();
        }

        public <T> Builder append(T text) {
            this.text.append(TextUtils.asText(text).get());
            return this;
        }

        public Builder style(ChatFormatting... styles) {
            for (var style : styles) {
                this.append(TextUtils.COLOR_CHAR).append(style.toString().substring(1).toUpperCase(Locale.ROOT));
            }
            return this;
        }

        public Builder color(Color color, ChatFormatting... styles) {
            return this.append(TextUtils.COLOR_CHAR)
                    .append(TextUtils.HEX_CHAR)
                    .append(String.format("%06X", 0xFFFFFF & color.getRGB()))
                    .styleSet(styles);
        }

        public Builder gradient(Color from, Color to, ChatFormatting... styles) {
            return this.append(TextUtils.COLOR_CHAR)
                    .append(TextUtils.HEX_CHAR)
                    .append(TextUtils.GRADIENT_CHAR)
                    .append(String.format("%06X", 0xFFFFFF & from.getRGB()))
                    .append("-")
                    .append(String.format("%06X", 0xFFFFFF & to.getRGB()))
                    .styleSet(styles);
        }

        private Builder styleSet(ChatFormatting... styles) {
            if (styles.length > 0) {
                this.append("[");
                for (var style : styles) {
                    this.append(style.toString().substring(1).toUpperCase(Locale.ROOT));
                }
                this.append("]");
            }
            return this;
        }

        public Text build() {
            return this.build(false);
        }

        public Text build(boolean immutable) {
            return immutable ? new Immutable(this.text.toString()) : new Text(this.text.toString());
        }
    }
}
