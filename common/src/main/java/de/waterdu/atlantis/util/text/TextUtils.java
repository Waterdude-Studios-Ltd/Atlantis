package de.waterdu.atlantis.util.text;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Sets;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.placeholder.AtlantisPlaceholderProxy;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Formats, colorizes, converts and joins text and components.
 */
public final class TextUtils {

    /**
     * Prefix of the numbered placeholders that {@link #format(String, Object...)} replaces, for example {@code %1}.
     */
    public static final String PLACEHOLDER_CHAR = "%";
    /**
     * Color code prefix that {@link #colorize(String)} accepts and converts to {@link #TRUE_COLOR_CHAR}.
     */
    public static final String COLOR_CHAR = "&";
    /**
     * Minecraft's own formatting code prefix.
     */
    public static final String TRUE_COLOR_CHAR = "§";
    /**
     * Marks a hex color code, for example {@code &#RRGGBB}.
     */
    public static final String HEX_CHAR = "#";
    /**
     * Marks a gradient color code, for example {@code &#@RRGGBB-RRGGBB}.
     */
    public static final String GRADIENT_CHAR = "@";
    public static final String HEX_PATTERN = TextUtils.TRUE_COLOR_CHAR + TextUtils.HEX_CHAR;
    public static final String GRADIENT_PATTERN = TextUtils.HEX_PATTERN + TextUtils.GRADIENT_CHAR;
    /**
     * {@link String#format} pattern that turns red, green and blue values from 0 to 255 into a hex color.
     */
    public static final String COLOR_FORMAT = "#%02x%02x%02x";
    public static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
    /**
     * Style array with every style off. This array is shared, so do not modify it.
     *
     * @see #getStyleArray(String)
     */
    public static final boolean[] NO_STYLE = new boolean[]{false, false, false, false, false};
    private static final Set<String> DO_NOT_CAPITALIZE = Sets.newHashSet("the", "a", "of", "and", "for", "nor", "but", "or", "yet", "so");

    /**
     * Always throws, because this class has only static members.
     */
    private TextUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Replaces {@code %1}, {@code %2} and so on with the matching argument.
     *
     * @param in the text that contains the placeholders
     * @param args the values for each placeholder, starting at {@code %1}. Components lose their styling.
     * @return a new {@link Text}
     */
    public static Text format(Text in, Object... args) {
        return TextUtils.format(in.get(), args);
    }

    /**
     * Replaces {@code %1}, {@code %2} and so on with the matching argument.
     *
     * @param in the text that contains the placeholders
     * @param args the values for each placeholder, starting at {@code %1}. Components lose their styling.
     * @return a new {@link Text}
     */
    public static Text format(String in, Object... args) {
        for (var i = args.length; i > 0; --i) {
            var arg = args[i-1];
            if (arg instanceof Component component) {
                in = in.replace(TextUtils.PLACEHOLDER_CHAR + i, component.getString());
            } else {
                in = in.replace(TextUtils.PLACEHOLDER_CHAR + i, String.valueOf(arg));
            }
        }
        return Text.of(in);
    }

    /**
     * Turns a string with color codes into a component. Codes can start with {@code &} or {@code §}.
     * <ul>
     *     <li>{@code &a}, {@code &l} and the other vanilla codes stay as legacy formatting.</li>
     *     <li>{@code &#RRGGBB} colors the text up to the next code.</li>
     *     <li>{@code &#@RRGGBB-RRGGBB[-RRGGBB...]} applies a gradient to the text up to the next code. It must have at least two colors.</li>
     *     <li>Each hex form can have style codes in brackets after it, for example {@code &#FF0000[lo]}. See {@link #getStyleArray(String)}.</li>
     * </ul>
     *
     * @param in the string to colorize
     * @return the colorized component, or an empty component if there is no text
     */
    public static MutableComponent colorize(String in) {
        in = in.replace(TextUtils.COLOR_CHAR, TextUtils.TRUE_COLOR_CHAR);
        MutableComponent component = null;
        var builder = new StringBuilder();
        var split = in.split(TextUtils.TRUE_COLOR_CHAR, -1);
        var first = in.startsWith(TextUtils.HEX_PATTERN);
        var first2 = in.startsWith(TextUtils.TRUE_COLOR_CHAR);
        for (var s : split) {
            var str = s;
            if (str.startsWith(TextUtils.HEX_CHAR)) {
                str = str.substring(1);
                if (!first) {
                    builder.append(s);
                    first = true;
                } else {
                    if (!builder.isEmpty()) {
                        if (component == null) {
                            component = Component.literal(builder.toString());
                        } else {
                            component.append(Component.literal(builder.toString()));
                        }
                        builder = new StringBuilder();
                    }
                    if (str.startsWith(TextUtils.GRADIENT_CHAR)) {
                        var pivots = new ArrayList<float[]>();
                        var index = 7;
                        var breakNext = false;
                        while (index < str.length()) {
                            pivots.add(Color.decode(TextUtils.HEX_CHAR + str.substring(index - 6, index)).getRGBColorComponents(null));
                            index += 7;
                            if (breakNext) {
                                index -= 7;
                                break;
                            }
                            if (index < str.length() && (str.charAt(index) == '@' || str.charAt(index) != '-')) {
                                breakNext = true;
                            }
                        }

                        str = str.substring(index);

                        boolean[] styles;
                        if (str.startsWith("[") && str.length() >= 3 && str.contains("]")) {
                            styles = TextUtils.getStyleArray(str.substring(str.indexOf("[") + 1, str.indexOf("]")));
                            str = str.substring(str.indexOf("]") + 1);
                        } else {
                            styles = TextUtils.NO_STYLE;
                        }

                        var chars = str.toCharArray();

                        if (pivots.size() > 1) {
                            var perPivot = (chars.length - pivots.size()) / (pivots.size() - 1F);
                            var bonus = (chars.length - pivots.size()) - (((int) perPivot) * (pivots.size() - 1));
                            var pivotValue = 0;
                            var pivot = 1;
                            var end = pivots.get(1);
                            var start = pivots.get(0);
                            for (var c : chars) {
                                var div = Mth.floor(perPivot) + (bonus > 0 ? 2F : 1F);
                                var r = (end[0] - start[0]) / div;
                                var g = (end[1] - start[1]) / div;
                                var b = (end[2] - start[2]) / div;
                                var segment = Component.literal(String.valueOf(c));
                                var color = new Color(start[0] + (r * pivotValue), start[1] + (g * pivotValue), start[2] + (b * pivotValue));
                                segment.setStyle(segment.getStyle().withColor(color.getRGB()));
                                TextUtils.applyStyleArray(segment, styles);
                                if (component == null) {
                                    component = segment;
                                } else {
                                    component.append(segment);
                                }
                                pivotValue += 1;
                                var test = pivotValue - perPivot;
                                if (((test >= 1 && test <= 2 && bonus-- > 0) || (test >= 0 && test <= 1 && bonus <= 0)) && ++pivot < pivots.size()) {
                                    start = end;
                                    end = pivots.get(pivot);
                                    pivotValue = 0;
                                }
                            }
                        }
                    } else {
                        var hex = TextUtils.HEX_CHAR + str.substring(0, 6);
                        str = str.substring(6);
                        boolean[] styles;
                        if (str.startsWith("[") && str.length() >= 3 && str.contains("]")) {
                            styles = TextUtils.getStyleArray(str.substring(str.indexOf("[") + 1, str.indexOf("]")));
                            str = str.substring(str.indexOf("]") + 1);
                        } else {
                            styles = TextUtils.NO_STYLE;
                        }
                        var segment = Component.literal(str);
                        segment.setStyle(segment.getStyle().withColor(TextColor.parseColor(hex).getOrThrow()));
                        TextUtils.applyStyleArray(segment, styles);
                        if (component == null) {
                            component = segment;
                        } else {
                            component.append(segment);
                        }
                    }
                }
            } else {
                if (!str.isEmpty()) {
                    builder.append(first2 ? TextUtils.TRUE_COLOR_CHAR : "").append(str);
                }
                first = true;
                first2 = true;
            }
        }
        if (!builder.isEmpty()) {
            if (component == null) {
                component = Component.literal(builder.toString());
            } else {
                component.append(Component.literal(builder.toString()));
            }
        }
        return component == null ? Component.empty() : component;
    }

    /**
     * @param components components to append as-is, or other objects to append as their {@link String#valueOf(Object)}
     * @return a new component with every argument appended in order
     */
    public static MutableComponent combine(Object... components) {
        var combined = Component.literal("");
        for (var object : components) {
            if (object instanceof Component component) {
                combined.append(component);
            } else {
                combined.append(String.valueOf(object));
            }
        }
        return combined;
    }

    /**
     * Removes line breaks, but keeps escaped {@code \n} sequences.
     */
    public static String stripNewlines(String in) {
        return in.replace("\\n", "\\NEWLINE").replace("\n", "").replace("\\NEWLINE", "\\n");
    }

    /**
     * @return the strings joined with spaces
     */
    public static String concat(String... strings) {
        return TextUtils.concat(0, " ", strings);
    }

    /**
     * @param start the index of the first string to include
     * @param delimiter the separator between strings
     * @param strings the strings to join
     * @return the strings from {@code start} onwards, joined with the delimiter
     */
    public static String concat(int start, String delimiter, String... strings) {
        return TextUtils.concat(start, strings.length, delimiter, strings);
    }

    /**
     * @param start the index of the first string to include
     * @param end the index after the last string to include
     * @param delimiter the separator between strings
     * @param strings the strings to join
     * @return the strings from {@code start} to {@code end}, joined with the delimiter
     */
    public static String concat(int start, int end, String delimiter, String... strings) {
        var builder = new StringBuilder();
        for (var i = start; i < strings.length; i++) {
            if (i >= end) {
                break;
            }
            if (!builder.isEmpty()) {
                builder.append(delimiter);
            }
            builder.append(strings[i]);
        }
        return builder.toString();
    }

    /**
     * Joins all strings, but replaces the string at {@code exceptIndex} with {@code exceptString}.
     *
     * @param exceptIndex the index of the string to replace
     * @param exceptString the string to put in its place
     * @param delimiter the separator between strings
     * @param strings the strings to join
     * @return the joined string
     */
    public static String concatExcept(int exceptIndex, String exceptString, String delimiter, String... strings) {
        var builder = new StringBuilder();
        for (var i = 0; i < strings.length; i++) {
            if (!builder.isEmpty()) {
                builder.append(delimiter);
            }
            if (i == exceptIndex) {
                builder.append(exceptString);
            } else {
                builder.append(strings[i]);
            }
        }
        return builder.toString();
    }

    /**
     * Removes every character that is not ASCII. ASCII punctuation stays.
     */
    public static String stripSpecialCharacters(String in) {
        return CharMatcher.ascii().retainFrom(in);
    }

    /**
     * Reads vanilla style codes into an array of obfuscated, bold, strikethrough, underlined and italic flags.
     *
     * @param codes any of {@code k}, {@code l}, {@code m}, {@code n} and {@code o}, in either case. {@code r} turns
     *     every style off.
     * @return the style array, or {@link #NO_STYLE}
     */
    public static boolean[] getStyleArray(String codes) {
        var chars = new HashSet<Character>();
        for (var c : codes.toCharArray()) {
            chars.add(c);
        }
        if (chars.contains('R') || chars.contains('r')) {
            return TextUtils.NO_STYLE;
        }
        return new boolean[]{
                chars.contains('K') || chars.contains('k'),
                chars.contains('L') || chars.contains('l'),
                chars.contains('M') || chars.contains('m'),
                chars.contains('N') || chars.contains('n'),
                chars.contains('O') || chars.contains('o')
        };
    }

    /**
     * Applies a style array to the component and all of its siblings.
     * This turns off any style that the array does not set.
     *
     * @param text the component to style
     * @param style an array from {@link #getStyleArray(String)}
     */
    public static void applyStyleArray(MutableComponent text, boolean[] style) {
        for (var sibling : text.getSiblings()) {
            if (sibling instanceof MutableComponent component) {
                TextUtils.applyStyleArray(component, style);
            }
        }
        text.setStyle(text.getStyle().withObfuscated(style[0]).withBold(style[1]).withStrikethrough(style[2]).withUnderlined(style[3]).withItalic(style[4]));
    }

    /**
     * @return a component holding a single space, not an empty one
     */
    public static MutableComponent empty() {
        return Component.literal(" ");
    }

    /**
     * Converts the object to {@link Text} as {@link #asText(Object)} does. Then replaces the placeholders for each
     * context in order. If no placeholder provider is installed, the placeholders do not change.
     *
     * @param textLike the text to convert
     * @param contexts the objects to resolve placeholders against, such as a player
     * @return the text with the placeholders replaced
     */
    public static Text asTextWithPlaceholders(Object textLike, Object... contexts) {
        var text = TextUtils.asText(textLike);
        for (var context : contexts) {
            text = AtlantisPlaceholderProxy.replace(context, text);
        }
        return text;
    }

    /**
     * Converts the object to {@link Text} as {@link #asText(Object)} does. Then replaces the placeholders for each
     * context in order. If no placeholder provider is installed, the placeholders do not change.
     *
     * @param textLike the text to convert
     * @param contexts the objects to resolve placeholders against, such as a player
     * @return the text with the placeholders replaced
     */
    public static Text asTextWithPlaceholders(Object textLike, Collection<Object> contexts) {
        var text = TextUtils.asText(textLike);
        for (var context : contexts) {
            text = AtlantisPlaceholderProxy.replace(context, text);
        }
        return text;
    }

    /**
     * @param textLike a {@link Text}, which is returned as-is, a component, which loses its styling, null, which gives
     *     empty text, or any other object, which is converted with toString
     * @return the object as {@link Text}
     */
    public static Text asText(Object textLike) {
        return switch (textLike) {
            case MutableComponent mutableComponent -> Text.of(mutableComponent);
            case Text text -> text;
            case null -> Text.of("");
            default -> Text.of(textLike.toString());
        };
    }

    /**
     * {@link #asTextWithPlaceholders(Object, Object...)}, compiled to a component.
     */
    public static MutableComponent asComponentWithPlaceholders(Object textLike, Object... contexts) {
        return TextUtils.asTextWithPlaceholders(textLike, contexts).compile();
    }

    /**
     * {@link #asTextWithPlaceholders(Object, Collection)}, compiled to a component.
     */
    public static MutableComponent asComponentWithPlaceholders(Object textLike, Collection<Object> contexts) {
        return TextUtils.asTextWithPlaceholders(textLike, contexts).compile();
    }

    /**
     * @param textLike a component, which is returned as-is, a {@link Text}, which is compiled, or any other object,
     *     which is converted with toString and colorized. Must not be null.
     * @return the object as a component
     */
    public static MutableComponent asComponent(Object textLike) {
        if (textLike instanceof MutableComponent component) {
            return component;
        } else if (textLike instanceof Text text) {
            return text.compile();
        } else {
            return Text.of(textLike.toString()).compile();
        }
    }

    /**
     * @param textLike a component, a {@link Text}, or any other object. Must not be null.
     * @return the component's plain text, the {@link Text}'s uncompiled string with its color codes, or the object's
     *     toString
     */
    public static String asString(Object textLike) {
        if (textLike instanceof MutableComponent component) {
            return component.getString();
        } else if (textLike instanceof Text text) {
            return text.get();
        } else {
            return textLike.toString();
        }
    }

    /**
     * Converts each element with {@link #asText(Object)}.
     * This method reuses {@link Text} elements, so they can be immutable.
     */
    public static Text[] toArray(Object[] textLikes) {
        var texts = new Text[textLikes.length];
        for (var i = 0; i < textLikes.length; i++) {
            texts[i] = TextUtils.asText(textLikes[i]);
        }
        return texts;
    }

    /**
     * Converts each element with {@link #asText(Object)} into a new, mutable {@link Text}.
     */
    public static Text[] toMutableArray(Object[] textLikes) {
        var texts = new Text[textLikes.length];
        for (var i = 0; i < textLikes.length; i++) {
            texts[i] = TextUtils.asText(textLikes[i]).mutable();
        }
        return texts;
    }

    /**
     * Joins two arrays into a new array.
     *
     * @param a1 the strings to put first
     * @param a2 the strings to put after them
     * @return a new array with the strings of {@code a1}, then the strings of {@code a2}
     */
    public static String[] join(String[] a1, String... a2) {
        var join = new String[a1.length + a2.length];
        System.arraycopy(a1, 0, join, 0, a1.length);
        System.arraycopy(a2, 0, join, a1.length, a2.length);
        return join;
    }

    /**
     * Counts how many times a character occurs in the string.
     *
     * @param string the string to search
     * @param test the character to count
     * @return the number of occurrences
     */
    public static int countCharOccurrence(String string, char test) {
        var count = 0;
        for (var c : string.toCharArray()) {
            if (c == test) {
                count++;
            }
        }
        return count;
    }

    /**
     * Use this to check that brackets are balanced.
     *
     * @return the number of {@code add} characters minus the number of {@code subtract} characters
     */
    public static int countCharOccurrenceDifference(String string, char add, char subtract) {
        var count = 0;
        for (var c : string.toCharArray()) {
            if (c == add) {
                count++;
            } else if (c == subtract) {
                count--;
            }
        }
        return count;
    }

    /**
     * Adds a word that {@link #capitalizeEachWord(String)} leaves in lower case. This applies to every mod.
     *
     * @param word the word, in lower case
     */
    public static void setNonCapitalizedWord(String word) {
        TextUtils.DO_NOT_CAPITALIZE.add(word);
    }

    /**
     * @see #capitalizeEachWord(String, Set)
     */
    public static Text capitalizeEachWord(Text in) {
        return TextUtils.capitalizeEachWord(in, TextUtils.DO_NOT_CAPITALIZE);
    }

    /**
     * @see #capitalizeEachWord(String, Set)
     */
    public static Text capitalizeEachWord(Text in, Set<String> doNotCapitalize) {
        return Text.of(TextUtils.capitalizeEachWord(in.get(), doNotCapitalize));
    }

    /**
     * Converts the string to title case. After the first word, short words such as "the" and "of" stay in lower case.
     *
     * @see #capitalizeEachWord(String, Set)
     */
    public static String capitalizeEachWord(String in) {
        return TextUtils.capitalizeEachWord(in, TextUtils.DO_NOT_CAPITALIZE);
    }

    /**
     * Converts the string to lower case, then capitalizes each word separated by a space.
     * The first word is always capitalized.
     *
     * @param in the string to capitalize
     * @param doNotCapitalize lower case words to leave as they are after the first word
     * @return the capitalized string
     */
    public static String capitalizeEachWord(String in, Set<String> doNotCapitalize) {
        var builder = new StringBuilder();
        var split = in.toLowerCase(Locale.ROOT).split(" ");
        var first = true;
        for (var word : split) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            var capitalize = first || !doNotCapitalize.contains(word);
            builder.append(capitalize ? StringUtils.capitalize(word) : word);
            first = false;
        }
        return builder.toString();
    }

    /**
     * Sets the click event on the component and its direct siblings, but not on their own siblings.
     *
     * @return the same component
     */
    public static MutableComponent applyClickEvent(MutableComponent text, ClickEvent event) {
        text.setStyle(text.getStyle().withClickEvent(event));
        for (var component : text.getSiblings()) {
            if (component instanceof MutableComponent sibling) {
                sibling.setStyle(sibling.getStyle().withClickEvent(event));
            }
        }
        return text;
    }

    /**
     * Sets the hover event on the component and its direct siblings, but not on their own siblings.
     *
     * @return the same component
     */
    public static MutableComponent applyHoverEvent(MutableComponent text, HoverEvent event) {
        text.setStyle(text.getStyle().withHoverEvent(event));
        for (var component : text.getSiblings()) {
            if (component instanceof MutableComponent sibling) {
                sibling.setStyle(sibling.getStyle().withHoverEvent(event));
            }
        }
        return text;
    }

    /**
     * @return whether {@link Double#parseDouble(String)} accepts the string, which includes signs, decimals, exponents
     *     and "NaN"
     */
    public static boolean isNumberParsable(String string) {
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @return whether every character is a digit from 0 to 9. True for an empty string.
     */
    public static boolean isNumber(String string) {
        for (var ch : string.toCharArray()) {
            if (!TextUtils.isNumeral(ch)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param ch the character to check
     * @return whether the character is a digit from 0 to 9
     */
    public static boolean isNumeral(char ch) {
        return ch >= '0' && ch <= '9';
    }
}
