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

public final class TextUtils {

    public static final String PLACEHOLDER_CHAR = "%";
    public static final String COLOR_CHAR = "&";
    public static final String TRUE_COLOR_CHAR = "§";
    public static final String HEX_CHAR = "#";
    public static final String GRADIENT_CHAR = "@";
    public static final String HEX_PATTERN = TextUtils.TRUE_COLOR_CHAR + TextUtils.HEX_CHAR;
    public static final String GRADIENT_PATTERN = TextUtils.HEX_PATTERN + TextUtils.GRADIENT_CHAR;
    public static final String COLOR_FORMAT = "#%02x%02x%02x";
    public static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
    public static final boolean[] NO_STYLE = new boolean[]{false, false, false, false, false};
    protected static final Set<String> DO_NOT_CAPITALIZE = Sets.newHashSet("the", "a", "of", "and", "for", "nor", "but", "or", "yet", "so");

    private TextUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static Text format(Text in, Object... args) {
        return TextUtils.format(in.get(), args);
    }

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
                    builder.append(str);
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

    public static String stripNewlines(String in) {
        return in.replace("\\n", "\\NEWLINE").replace("\n", "").replace("\\NEWLINE", "\\n");
    }

    public static String concat(String... strings) {
        return TextUtils.concat(0, " ", strings);
    }

    public static String concat(int start, String delimiter, String... strings) {
        return TextUtils.concat(start, strings.length, delimiter, strings);
    }

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

    public static String stripSpecialCharacters(String in) {
        return CharMatcher.ascii().retainFrom(in);
    }

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

    public static void applyStyleArray(MutableComponent text, boolean[] style) {
        for (var sibling : text.getSiblings()) {
            if (sibling instanceof MutableComponent component) {
                TextUtils.applyStyleArray(component, style);
            }
        }
        text.setStyle(text.getStyle().withObfuscated(style[0]).withBold(style[1]).withStrikethrough(style[2]).withUnderlined(style[3]).withItalic(style[4]));
    }

    public static MutableComponent empty() {
        return Component.literal(" ");
    }

    public static Text asTextWithPlaceholders(Object textLike, Object... contexts) {
        var text = TextUtils.asText(textLike);
        for (var context : contexts) {
            text = AtlantisPlaceholderProxy.replace(context, text);
        }
        return text;
    }

    public static Text asTextWithPlaceholders(Object textLike, Collection<Object> contexts) {
        var text = TextUtils.asText(textLike);
        for (var context : contexts) {
            text = AtlantisPlaceholderProxy.replace(context, text);
        }
        return text;
    }

    public static Text asText(Object textLike) {
        return switch (textLike) {
            case MutableComponent mutableComponent -> Text.of(mutableComponent);
            case Text text -> text;
            case null -> Text.of("");
            default -> Text.of(textLike.toString());
        };
    }

    public static MutableComponent asComponentWithPlaceholders(Object textLike, Object... contexts) {
        return TextUtils.asTextWithPlaceholders(textLike, contexts).compile();
    }

    public static MutableComponent asComponentWithPlaceholders(Object textLike, Collection<Object> contexts) {
        return TextUtils.asTextWithPlaceholders(textLike, contexts).compile();
    }

    public static MutableComponent asComponent(Object textLike) {
        if (textLike instanceof MutableComponent component) {
            return component;
        } else if (textLike instanceof Text text) {
            return text.compile();
        } else {
            return Text.of(textLike.toString()).compile();
        }
    }

    public static String asString(Object textLike) {
        if (textLike instanceof MutableComponent component) {
            return component.getString();
        } else if (textLike instanceof Text text) {
            return text.get();
        } else {
            return textLike.toString();
        }
    }

    public static Text[] toArray(Object[] textLikes) {
        var texts = new Text[textLikes.length];
        for (var i = 0; i < textLikes.length; i++) {
            texts[i] = TextUtils.asText(textLikes[i]);
        }
        return texts;
    }

    public static Text[] toMutableArray(Object[] textLikes) {
        var texts = new Text[textLikes.length];
        for (var i = 0; i < textLikes.length; i++) {
            texts[i] = TextUtils.asText(textLikes[i]).mutable();
        }
        return texts;
    }

    public static String[] join(String[] a1, String... a2) {
        var join = new String[a1.length + a2.length];
        System.arraycopy(a1, 0, join, 0, a1.length);
        System.arraycopy(a2, 0, join, a1.length, a2.length);
        return join;
    }

    public static int countCharOccurrence(String string, char test) {
        var count = 0;
        for (var c : string.toCharArray()) {
            if (c == test) {
                count++;
            }
        }
        return count;
    }

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

    public static void setNonCapitalizedWord(String word) {
        TextUtils.DO_NOT_CAPITALIZE.add(word);
    }

    public static Text capitalizeEachWord(Text in) {
        return TextUtils.capitalizeEachWord(in, TextUtils.DO_NOT_CAPITALIZE);
    }

    public static Text capitalizeEachWord(Text in, Set<String> doNotCapitalize) {
        return Text.of(TextUtils.capitalizeEachWord(in.get(), doNotCapitalize));
    }

    public static String capitalizeEachWord(String in) {
        return TextUtils.capitalizeEachWord(in, TextUtils.DO_NOT_CAPITALIZE);
    }

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

    public static MutableComponent applyClickEvent(MutableComponent text, ClickEvent event) {
        for (var component : text.getSiblings()) {
            if (component instanceof MutableComponent sibling) {
                sibling.setStyle(sibling.getStyle().withClickEvent(event));
            }
        }
        return text;
    }

    public static MutableComponent applyHoverEvent(MutableComponent text, HoverEvent event) {
        for (var component : text.getSiblings()) {
            if (component instanceof MutableComponent sibling) {
                sibling.setStyle(sibling.getStyle().withHoverEvent(event));
            }
        }
        return text;
    }

    public static boolean isNumberParsable(String string) {
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNumber(String string) {
        for (var ch : string.toCharArray()) {
            if (!TextUtils.isNumeral(ch)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNumeral(char ch) {
        return ch >= '0' && ch <= '9';
    }
}
