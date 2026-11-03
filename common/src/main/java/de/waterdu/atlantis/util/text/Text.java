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

/**
 * Holds a string with color codes that compiles to a component. This text is mutable: methods such as
 * {@link #append(Object)} change it and return it. {@link Immutable} returns a copy instead.
 */
@NullMarked
public class Text implements Serializable, Comparable<Text>, CharSequence {

    private String value;
    boolean raw = false;
    boolean translate = false;

    /**
     * Creates a mutable text.
     *
     * @param value the uncompiled value, which can contain color codes
     */
    protected Text(String value) {
        this.value = value;
    }

    /**
     * @see TextUtils#asText(Object)
     */
    public static Text of(Object stringLike) {
        return TextUtils.asText(stringLike);
    }

    /**
     * @param string the uncompiled value, which can contain color codes
     * @return a new mutable {@link Text} with the value
     */
    public static Text of(String string) {
        return new Text(string);
    }

    /**
     * @param component the component to read. Its styling is lost.
     * @return a new mutable {@link Text} with the component's plain text
     */
    public static Text of(Component component) {
        return Text.of(component.getString());
    }

    /**
     * @return an immutable copy, without the raw and translatable flags
     */
    public static Text of(Text text) {
        return text.immutable();
    }

    /**
     * @return a new, empty {@link Immutable}
     */
    public static Text empty() {
        return new Immutable("");
    }

    /**
     * @return a mutable copy, without the raw and translatable flags
     */
    public Text mutable() {
        return Text.of(this.get());
    }

    /**
     * @return an immutable copy, without the raw and translatable flags
     */
    public Immutable immutable() {
        return Immutable.of(this.get());
    }

    /**
     * Makes {@link #compile()} ignore color codes. Changes this text, or returns a mutable copy if it is {@link Immutable}.
     */
    public Text raw() {
        this.raw = true;
        return this;
    }

    /**
     * Undoes {@link #raw()}. Changes this text, or returns a mutable copy if it is {@link Immutable}.
     */
    public Text compiled() {
        this.raw = false;
        return this;
    }

    /**
     * Makes {@link #compile()} treat the value as a translation key and ignore color codes.
     * Changes this text, or returns a mutable copy if it is {@link Immutable}.
     */
    public Text translatable() {
        this.translate = true;
        return this;
    }

    /**
     * Changes this text and returns it, or returns a new {@link Immutable} if this one is immutable.
     */
    public Text set(String string) {
        this.value = string;
        return this;
    }

    /**
     * Changes this text and returns it, or returns a new {@link Immutable} if this one is immutable.
     */
    public Text clear() {
        this.value = "";
        return this;
    }

    /**
     * Changes this text and returns it, or returns a new {@link Immutable} if this one is immutable.
     */
    public Text append(Object string) {
        this.value = this.value + string;
        return this;
    }

    /**
     * Changes this text and returns it, or returns a new {@link Immutable} if this one is immutable.
     */
    public Text prepend(Object string) {
        this.value = string + this.value;
        return this;
    }

    /**
     * Splits the uncompiled value around matches of the regular expression.
     *
     * @param regex the regular expression to split around
     * @return the parts, as from {@link String#split(String)}
     */
    public String[] split(String regex) {
        return this.value.split(regex);
    }

    /**
     * Changes this text and returns it, or returns a new {@link Immutable} if this one is immutable.
     */
    public Text set(Text string) {
        this.value = string.value;
        return this;
    }

    /**
     * Changes this text and returns it, or returns a new {@link Immutable} if this one is immutable.
     */
    public Text append(Text string) {
        this.value = this.value + string.value;
        return this;
    }

    /**
     * Changes this text and returns it, or returns a new {@link Immutable} if this one is immutable.
     */
    public Text prepend(Text string) {
        this.value = string.value + this.value;
        return this;
    }

    /**
     * Splits the uncompiled value around matches of the regular expression.
     *
     * @param regex the text that holds the regular expression to split around
     * @return the parts, as from {@link String#split(String)}
     */
    public String[] split(Text regex) {
        return this.value.split(regex.value);
    }

    /**
     * @param s the sequence to search for
     * @return whether the uncompiled value contains the sequence
     */
    public boolean contains(CharSequence s) {
        return this.value.contains(s);
    }

    /**
     * @param string the string to compare with
     * @return whether the uncompiled value equals the string, ignoring case
     */
    public boolean equalsIgnoreCase(String string) {
        return this.value.equalsIgnoreCase(string);
    }

    /**
     * @param string the text to compare with
     * @return whether the uncompiled values are equal, ignoring case
     */
    public boolean equalsIgnoreCase(Text string) {
        return this.value.equalsIgnoreCase(string.value);
    }

    /**
     * @return whether the uncompiled value has no characters
     */
    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    /**
     * Unlike {@link #append(Object)}, never changes this text.
     *
     * @return a new mutable {@link Text}, without the raw and translatable flags
     */
    public Text replace(CharSequence target, CharSequence replacement) {
        return Text.of(this.value.replace(target, replacement));
    }

    /**
     * Uses the default locale. Unlike {@link #append(Object)}, never changes this text.
     *
     * @return a new mutable {@link Text}, without the raw and translatable flags
     */
    public Text toLowerCase() {
        return Text.of(this.value.toLowerCase());
    }

    /**
     * Uses the default locale. Unlike {@link #append(Object)}, never changes this text.
     *
     * @return a new mutable {@link Text}, without the raw and translatable flags
     */
    public Text toUpperCase() {
        return Text.of(this.value.toUpperCase());
    }

    /**
     * Unlike {@link #append(Object)}, never changes this text.
     *
     * @return a new mutable {@link Text}, without the raw and translatable flags
     */
    public Text trim() {
        return Text.of(this.value.trim());
    }

    /**
     * @param prefix the prefix to check for
     * @return whether the uncompiled value starts with the prefix
     */
    public boolean startsWith(String prefix) {
        return this.value.startsWith(prefix);
    }

    /**
     * @param prefix the prefix to check for
     * @param offset the index in the value where the prefix must start
     * @return whether the uncompiled value has the prefix at the offset
     */
    public boolean startsWith(String prefix, int offset) {
        return this.value.startsWith(prefix, offset);
    }

    /**
     * @param suffix the suffix to check for
     * @return whether the uncompiled value ends with the suffix
     */
    public boolean endsWith(String suffix) {
        return this.value.endsWith(suffix);
    }

    /**
     * @param prefix the text that holds the prefix to check for
     * @return whether the uncompiled value starts with the uncompiled value of the prefix
     */
    public boolean startsWith(Text prefix) {
        return this.value.startsWith(prefix.value);
    }

    /**
     * @param prefix the text that holds the prefix to check for
     * @param offset the index in the value where the prefix must start
     * @return whether the uncompiled value has the uncompiled value of the prefix at the offset
     */
    public boolean startsWith(Text prefix, int offset) {
        return this.value.startsWith(prefix.value, offset);
    }

    /**
     * @param suffix the text that holds the suffix to check for
     * @return whether the uncompiled value ends with the uncompiled value of the suffix
     */
    public boolean endsWith(Text suffix) {
        return this.value.endsWith(suffix.value);
    }

    /**
     * @return the uncompiled value, including color codes
     */
    public String get() {
        return this.value;
    }

    /**
     * @return a new {@link Text} with {@code %1}, {@code %2} and so on replaced
     * @see TextUtils#format(String, Object...)
     */
    public Text format(Object... args) {
        return TextUtils.format(this.value, args);
    }

    /**
     * @see TextUtils#asTextWithPlaceholders(Object, Object...)
     */
    public Text formatPlaceholders(Object... contexts) {
        return TextUtils.asTextWithPlaceholders(this, contexts);
    }

    /**
     * @see TextUtils#asTextWithPlaceholders(Object, Collection)
     */
    public Text formatPlaceholders(Collection<Object> contexts) {
        return TextUtils.asTextWithPlaceholders(this, contexts);
    }

    /**
     * Compiles this text and sends it to each receiver.
     */
    public void send(CommandSourceStack... receivers) {
        MessageUtils.send(this.compile(), receivers);
    }

    /**
     * Compiles this text and sends it to each receiver.
     */
    public void send(CommandSource... receivers) {
        MessageUtils.send(this.compile(), receivers);
    }

    /**
     * Treats this text as a command. Give the command a source and placeholders before it executes.
     */
    public Command.CommandContext prepareCommand() {
        return Command.of(this).prepareContext();
    }

    /**
     * Compiles to a component. Empty text becomes a single space.
     *
     * @see #compile(boolean)
     */
    public MutableComponent compile() {
        return this.compile(true);
    }

    /**
     * Compiles to a component. The component is translatable if {@link #translatable()} was called. It is literal if
     * {@link #raw()} was called. Otherwise, {@link TextUtils#colorize(String)} applies the color codes.
     *
     * @param ifEmptyAddSpace whether empty text becomes a single space. Ignored for translatable text.
     * @return a new component
     */
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

    /**
     * {@link #compile()} with the event set on the component and its direct siblings.
     */
    public MutableComponent compileWithEvent(ClickEvent.Action action, String value) {
        return TextUtils.applyClickEvent(this.compile(), new ClickEvent(action, value));
    }

    /**
     * {@link #compile()} with the event set on the component and its direct siblings.
     */
    public <T> MutableComponent compileWithEvent(HoverEvent.Action<T> action, T value) {
        return TextUtils.applyHoverEvent(this.compile(), new HoverEvent(action, value));
    }

    /**
     * {@link #compile()} with both events set on the component and its direct siblings.
     */
    public <T> MutableComponent compileWithEvents(ClickEvent.Action clickEventAction, String clickEventValue,
                                                           HoverEvent.Action<T> hoverEventAction, T hoverEventValue) {
        return TextUtils.applyHoverEvent(TextUtils.applyClickEvent(this.compile(), new ClickEvent(clickEventAction, clickEventValue)), new HoverEvent(hoverEventAction, hoverEventValue));
    }

    /**
     * @return a mutable copy, without the raw and translatable flags
     */
    public Text copy() {
        return Text.of(this.value);
    }

    /**
     * @return the hash code of the uncompiled value
     */
    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    /**
     * Also true for a {@link String} with the same value. But the string does not consider itself equal to this text.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Text) {
            return this.value.equals(((Text) obj).value);
        }
        return this.value.equals(obj);
    }

    /**
     * @return the length of the uncompiled value, including color codes
     */
    @Override
    public int length() {
        return this.value.length();
    }

    /**
     * @param index the index of the character
     * @return the character at the index of the uncompiled value
     */
    @Override
    public char charAt(int index) {
        return this.value.charAt(index);
    }

    /**
     * @param start the index of the first character, inclusive
     * @param end the index after the last character
     * @return the part of the uncompiled value, as a {@link String}
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return this.value.subSequence(start, end);
    }

    /**
     * @return the uncompiled value, including color codes
     */
    @Override
    public String toString() {
        return this.value;
    }

    /**
     * @return the characters of the uncompiled value
     */
    @Override
    public IntStream chars() {
        return this.value.chars();
    }

    /**
     * @return the code points of the uncompiled value
     */
    @Override
    public IntStream codePoints() {
        return this.value.codePoints();
    }

    /**
     * Compares the uncompiled values lexicographically.
     *
     * @param text the text to compare with
     * @return a negative number, zero or a positive number if this text is less than, equal to or greater than
     *     the other text
     */
    @Override
    public int compareTo(Text text) {
        return this.value.compareTo(text.value);
    }

    /**
     * Stores the value as a plain string. Thus, the raw and translatable flags are lost.
     */
    public static class TypeAdapter implements JsonSerializer<Text>, JsonDeserializer<Text> {

        /**
         * Reads a mutable {@link Text} from a JSON string.
         *
         * @param json the JSON string
         * @param typeOfT the type to read
         * @param context the deserialization context
         * @return a new mutable {@link Text}
         */
        @Override
        public Text deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Text.of(json.getAsString());
        }

        /**
         * Writes the uncompiled value as a JSON string.
         *
         * @param src the text to write
         * @param typeOfSrc the type to write
         * @param context the serialization context
         * @return a JSON string
         */
        @Override
        public JsonElement serialize(Text src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    /**
     * Holds text that never changes. Methods that change a mutable {@link Text} return a copy instead.
     */
    @NullMarked
    public static class Immutable extends Text {

        /**
         * Creates an immutable text.
         *
         * @param value the uncompiled value, which can contain color codes
         */
        protected Immutable(String value) {
            super(value);
        }

        /**
         * @param string the uncompiled value, which can contain color codes
         * @return a new {@link Immutable} with the value
         */
        public static Immutable of(String string) {
            return new Immutable(string);
        }

        /**
         * @param component the component to read. Its styling is lost.
         * @return a new {@link Immutable} with the component's plain text
         */
        public static Immutable of(Component component) {
            return Immutable.of(component.getString());
        }

        /**
         * @return a new, empty {@link Immutable}
         */
        public static Immutable empty() {
            return new Immutable("");
        }

        /**
         * Makes a mutable copy that {@link #compile()} compiles without color codes.
         *
         * @return a new mutable {@link Text} with the raw flag set
         */
        @Override
        public Text raw() {
            var copy = this.copy();
            copy.raw = true;
            return copy;
        }

        /**
         * Makes a mutable copy with the raw flag cleared.
         *
         * @return a new mutable {@link Text}
         */
        @Override
        public Text compiled() {
            var copy = this.copy();
            copy.raw = false;
            return copy;
        }

        /**
         * Makes a mutable copy that {@link #compile()} treats as a translation key.
         *
         * @return a new mutable {@link Text} with the translatable flag set
         */
        @Override
        public Text translatable() {
            var copy = this.copy();
            copy.translate = true;
            return copy;
        }

        /**
         * @param string the new value
         * @return a new {@link Immutable} with the value. This text does not change.
         */
        @Override
        public Text set(String string) {
            return Immutable.of(string);
        }

        /**
         * @return a new, empty {@link Immutable}. This text does not change.
         */
        @Override
        public Text clear() {
            return Immutable.empty();
        }

        /**
         * @param string the object to add after the value, as its {@link String#valueOf(Object)}
         * @return a new {@link Immutable} with the object appended. This text does not change.
         */
        @Override
        public Text append(Object string) {
            return Immutable.of(this.get() + string);
        }

        /**
         * @param string the object to add before the value, as its {@link String#valueOf(Object)}
         * @return a new {@link Immutable} with the object prepended. This text does not change.
         */
        @Override
        public Text prepend(Object string) {
            return Immutable.of(string + this.get());
        }

        /**
         * @param string the text whose uncompiled value to use
         * @return a new {@link Immutable} with the value. This text does not change.
         */
        public Text set(Text string) {
            return this.set(string.get());
        }

        /**
         * @param string the text whose uncompiled value to add after the value
         * @return a new {@link Immutable} with the text appended. This text does not change.
         */
        public Text append(Text string) {
            return this.append(string.get());
        }

        /**
         * @param string the text whose uncompiled value to add before the value
         * @return a new {@link Immutable} with the text prepended. This text does not change.
         */
        public Text prepend(Text string) {
            return this.prepend(string.get());
        }

        /**
         * Stores the value as a plain string. If either flag is set, stores the value as an object with
         * {@code value}, {@code raw} and {@code translate}.
         */
        public static class TypeAdapter implements JsonSerializer<Immutable>, JsonDeserializer<Immutable> {

            /**
             * Reads an {@link Immutable} from a JSON string, or from an object with {@code value} and the optional
             * {@code raw} and {@code translate} flags.
             *
             * @param json the JSON string or object
             * @param typeOfT the type to read
             * @param context the deserialization context
             * @return a new {@link Immutable}
             */
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

            /**
             * Writes the uncompiled value as a JSON string. If either flag is set, writes an object with the value
             * and both flags instead.
             *
             * @param src the text to write
             * @param typeOfSrc the type to write
             * @param context the serialization context
             * @return a JSON string or object
             */
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

    /**
     * @return a builder for text with color codes
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link Text} from parts and color codes.
     */
    @NullMarked
    public static class Builder {

        private final StringBuilder text;

        /**
         * Creates an empty builder.
         */
        public Builder() {
            this.text = new StringBuilder();
        }

        /**
         * @param text anything {@link TextUtils#asText(Object)} accepts. Components lose their styling.
         * @return this builder
         */
        public <T> Builder append(T text) {
            this.text.append(TextUtils.asText(text).get());
            return this;
        }

        /**
         * Appends a vanilla color or style code for each format.
         */
        public Builder style(ChatFormatting... styles) {
            for (var style : styles) {
                this.append(TextUtils.COLOR_CHAR).append(style.toString().substring(1).toUpperCase(Locale.ROOT));
            }
            return this;
        }

        /**
         * Appends a hex color code. The code applies until the next code.
         *
         * @param color the color
         * @param styles styles to apply with the color
         * @return this builder
         */
        public Builder color(Color color, ChatFormatting... styles) {
            return this.append(TextUtils.COLOR_CHAR)
                    .append(TextUtils.HEX_CHAR)
                    .append(String.format("%06X", 0xFFFFFF & color.getRGB()))
                    .styleSet(styles);
        }

        /**
         * Appends a gradient code. The gradient spreads over the text until the next code.
         *
         * @param from the color of the first character
         * @param to the color of the last character
         * @param styles styles to apply with the gradient
         * @return this builder
         */
        public Builder gradient(Color from, Color to, ChatFormatting... styles) {
            return this.append(TextUtils.COLOR_CHAR)
                    .append(TextUtils.HEX_CHAR)
                    .append(TextUtils.GRADIENT_CHAR)
                    .append(String.format("%06X", 0xFFFFFF & from.getRGB()))
                    .append("-")
                    .append(String.format("%06X", 0xFFFFFF & to.getRGB()))
                    .styleSet(styles);
        }

        /**
         * Appends the style codes of the formats in brackets, for example {@code [LO]}. Appends nothing if there are
         * no formats.
         *
         * @param styles the formats to append
         * @return this builder
         */
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

        /**
         * @return a new mutable {@link Text}
         */
        public Text build() {
            return this.build(false);
        }

        /**
         * @param immutable whether to build an {@link Immutable}
         * @return a new {@link Text}
         */
        public Text build(boolean immutable) {
            return immutable ? new Immutable(this.text.toString()) : new Text(this.text.toString());
        }
    }
}
