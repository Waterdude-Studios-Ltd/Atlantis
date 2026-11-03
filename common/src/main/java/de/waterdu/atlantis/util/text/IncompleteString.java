package de.waterdu.atlantis.util.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Holds a string as fixed segments and empty placeholders. The {@code complete} methods fill the placeholders in
 * order.
 */
public class IncompleteString {

    private final String[] strings;
    private final boolean[] order;
    private final boolean wrapper;

    /**
     * Creates an incomplete string.
     *
     * @param strings the fixed segments
     * @param order {@code true} for a segment and {@code false} for a placeholder, in order. {@code null} makes the
     *     instance a wrapper of the first segment without placeholders.
     */
    private IncompleteString(String[] strings, boolean[] order) {
        this.strings = strings;
        this.order = order;
        this.wrapper = order == null;
    }

    /**
     * @return a new instance with copies of the segments and the placeholders
     */
    public IncompleteString copy() {
        if (this.wrapper) {
            return new IncompleteString(new String[]{this.strings[0]}, null);
        } else {
            var strings = new String[this.strings.length];
            var order = new boolean[this.order.length];
            System.arraycopy(this.strings, 0, strings, 0, this.strings.length);
            System.arraycopy(this.order, 0, order, 0, this.order.length);
            return new IncompleteString(strings, order);
        }
    }

    /**
     * Changes this instance and returns it. Use {@link #copy()} first to keep the original.
     */
    public IncompleteString replace(CharSequence target, CharSequence replacement) {
        for (var i = 0; i < this.strings.length; i++) {
            this.strings[i] = this.strings[i].replace(target, replacement);
        }
        return this;
    }

    /**
     * Inserts {@code replacement} in place of {@code target}. A segment changes only if it contains {@code target} exactly once.
     *
     * @param target a regular expression
     * @return a new instance. This instance does not change.
     */
    public IncompleteString replace(String target, IncompleteString replacement) {
        var builder = IncompleteString.builder();
        if (this.wrapper) {
            var split = this.strings[0].split(target, -1);
            if (split.length == 2) {
                builder.string(split[0]).merge(replacement).string(split[1]);
            } else {
                builder.string(this.strings[0]);
            }
        } else {
            var i = 0;
            for (var order : this.order) {
                if (order) {
                    var split = this.strings[i].split(target, -1);
                    if (split.length == 2) {
                        builder.string(split[0]).merge(replacement).string(split[1]);
                    } else {
                        builder.string(this.strings[i]);
                    }
                    i++;
                } else {
                    builder.placeholder();
                }
            }
        }
        return builder.build();
    }

    /**
     * Fills the placeholders in order. A placeholder without a value stays empty. This method ignores extra values.
     *
     * @param placeholders the values for the placeholders
     * @return the completed string
     */
    public String complete(String... placeholders) {
        if (this.wrapper) {
            return this.strings[0];
        }
        var builder = new StringBuilder();
        var a = 0;
        var b = 0;
        for (var order : this.order) {
            if (order) {
                builder.append(this.strings[a++]);
            } else {
                if (placeholders.length > b) {
                    builder.append(placeholders[b++]);
                }
            }
        }
        return builder.toString();
    }

    /**
     * Fills the placeholders in order. A placeholder without a value stays empty. This method ignores extra values.
     */
    public String complete(Object... placeholders) {
        if (this.wrapper) {
            return this.strings[0];
        }
        var builder = new StringBuilder();
        var a = 0;
        var b = 0;
        for (var order : this.order) {
            if (order) {
                builder.append(this.strings[a++]);
            } else {
                if (placeholders.length > b) {
                    builder.append(placeholders[b++]);
                }
            }
        }
        return builder.toString();
    }

    /**
     * Fills the placeholders in order, as a {@link Text}. A placeholder without a value stays empty. This method
     * ignores extra values.
     *
     * @param placeholders the values for the placeholders
     * @return the completed text
     */
    public Text completeAsText(Object... placeholders) {
        if (this.wrapper) {
            return Text.of(this.strings[0]);
        }
        var text = Text.empty().mutable();
        var a = 0;
        var b = 0;
        for (var order : this.order) {
            if (order) {
                text.append(this.strings[a++]);
            } else {
                if (placeholders.length > b) {
                    text.append(placeholders[b++]);
                }
            }
        }
        return text.immutable();
    }

    /**
     * Fills the placeholders in order, as a component. Each value is converted with {@link TextUtils#asComponent}. A
     * placeholder without a value stays empty. This method ignores extra values.
     *
     * @param placeholders the values for the placeholders
     * @return the completed component
     */
    public MutableComponent completeAsComponent(Object... placeholders) {
        if (this.wrapper) {
            return Component.literal(this.strings[0]);
        }
        var text = Component.literal("");
        var a = 0;
        var b = 0;
        for (var order : this.order) {
            if (order) {
                text.append(this.strings[a++]);
            } else {
                if (placeholders.length > b) {
                    text.append(TextUtils.asComponent(placeholders[b++]));
                }
            }
        }
        return text;
    }

    /**
     * @return a new, empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an incomplete string without placeholders.
     *
     * @param string the string
     * @return the incomplete string
     */
    public static IncompleteString of(String string) {
        return new IncompleteString(new String[]{string}, null);
    }

    /**
     * Builds an {@link IncompleteString} from segments and placeholders.
     */
    public static class Builder {

        private final List<String> strings = new ArrayList<>();
        private int count = 0;

        /**
         * Creates an empty builder.
         */
        private Builder() {}

        /**
         * Checks if the builder has no segments and no placeholders.
         *
         * @return {@code true} if the builder is empty
         */
        public boolean isEmpty() {
            return this.strings.isEmpty();
        }

        /**
         * Adds a placeholder at the end.
         *
         * @return this builder
         */
        public Builder placeholder() {
            this.strings.add(null);
            return this;
        }

        /**
         * Adds a new segment at the end.
         *
         * @param string the segment. Must not be {@code null}.
         * @return this builder
         */
        public Builder string(String string) {
            this.strings.add(Objects.requireNonNull(string));
            this.count++;
            return this;
        }

        /**
         * Adds the character to the end with {@link #append(String)}.
         *
         * @param c the character to add
         * @return this builder
         */
        public Builder append(char c) {
            return this.append(String.valueOf(c));
        }

        /**
         * Adds the string to the last segment. If the builder is empty or ends with a placeholder, it adds a new
         * segment.
         *
         * @param string the string to add
         * @return this builder
         */
        public Builder append(String string) {
            if (this.isEmpty()) {
                this.string(string);
            } else {
                var i = this.strings.size() - 1;
                var s = this.strings.get(i);
                if (s == null) {
                    this.string(string);
                } else {
                    this.strings.set(i, s + string);
                }
            }
            return this;
        }

        /**
         * Adds the segments and placeholders of the incomplete string at the end, as new entries.
         *
         * @param string the incomplete string to add
         * @return this builder
         */
        public Builder merge(IncompleteString string) {
            if (string.order == null) {
                this.string(string.strings[0]);
            } else {
                var i = 0;
                for (boolean order : string.order) {
                    if (order) {
                        this.string(string.strings[i++]);
                    } else {
                        this.placeholder();
                    }
                }
            }
            return this;
        }

        /**
         * Creates the incomplete string. A single segment without placeholders gives the same result as
         * {@link IncompleteString#of}.
         *
         * @return the incomplete string
         */
        public IncompleteString build() {
            var strings = new String[this.count];
            var order = new boolean[this.strings.size()];
            if (this.strings.size() == 1 && this.strings.getFirst() != null) {
                return IncompleteString.of(this.strings.getFirst());
            }
            var s = 0;
            for (var i = 0; i < order.length; i++) {
                var string = this.strings.get(i);
                if (string == null) {
                    order[i] = false;
                } else {
                    order[i] = true;
                    strings[s++] = string;
                }
            }
            return new IncompleteString(strings, order);
        }
    }
}
