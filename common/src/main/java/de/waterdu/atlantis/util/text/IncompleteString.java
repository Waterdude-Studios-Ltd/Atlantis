package de.waterdu.atlantis.util.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IncompleteString {

    private final String[] strings;
    private final boolean[] order;
    private final boolean wrapper;

    private IncompleteString(String[] strings, boolean[] order) {
        this.strings = strings;
        this.order = order;
        this.wrapper = order == null;
    }

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

    public IncompleteString replace(CharSequence target, CharSequence replacement) {
        for (var i = 0; i < this.strings.length; i++) {
            this.strings[i] = this.strings[i].replace(target, replacement);
        }
        return this;
    }

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

    public static Builder builder() {
        return new Builder();
    }

    public static IncompleteString of(String string) {
        return new IncompleteString(new String[]{string}, null);
    }

    public static class Builder {

        private final List<String> strings = new ArrayList<>();
        private int count = 0;

        private Builder() {}

        public boolean isEmpty() {
            return this.strings.isEmpty();
        }

        public Builder placeholder() {
            this.strings.add(null);
            return this;
        }

        public Builder string(String string) {
            this.strings.add(Objects.requireNonNull(string));
            this.count++;
            return this;
        }

        public Builder append(char c) {
            return this.append(String.valueOf(c));
        }

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

        public IncompleteString build() {
            var strings = new String[this.count];
            var order = new boolean[this.strings.size()];
            if (this.strings.size() == 1 && this.strings.get(0) != null) {
                return IncompleteString.of(this.strings.get(0));
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
