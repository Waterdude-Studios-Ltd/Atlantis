package de.waterdu.atlantis.file;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Converts configs between JSON and Atlantis Object Notation, and adds the {@link AonComment} comments.
 */
public final class AonCommenter {

    private static final String NEWLINE = "\n";

    private static final Pattern JAVA_TO_JSON_FIELD = Pattern.compile("([a-z])([A-Z])");
    private static final Pattern JSON_TO_JAVA_FIELD = Pattern.compile("(_)([a-z])");

    private static final Set<Class<? extends Configuration>> NOT_COMMENTED = Sets.newConcurrentHashSet();
    private static final Map<Class<? extends Configuration>, Info> INFO_MAP = new ConcurrentHashMap<>();

    private AonCommenter() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Converts JSON to Atlantis Object Notation and adds the {@link AonComment} comments of the type.
     * Returns the JSON unchanged if the config is not AON or the type has no {@link AonComment}.
     */
    public static String comment(Config<?> config, String json, Class<? extends Configuration> type) {
        if (!config.isAON()) {
            return json;
        }

        if (AonCommenter.NOT_COMMENTED.contains(type)) {
            return json;
        }

        Info info;
        if (AonCommenter.INFO_MAP.containsKey(type)) {
            info = AonCommenter.INFO_MAP.get(type);
        } else {
            var root = type.getAnnotation(AonComment.class);
            if (root == null) {
                AonCommenter.NOT_COMMENTED.add(type);
                return json;
            }
            info = new Info(root, type);
            AonCommenter.INFO_MAP.put(type, info);
        }
        return info.comment(json);
    }

    /**
     * Converts Atlantis Object Notation back to JSON.
     * Returns the input unchanged if the config is not AON or the input starts with <code>{</code>.
     */
    public static String uncomment(Config<?> config, String json, Class<? extends Configuration> type) {
        if (!config.isAON()) {
            return json;
        }
        if (AonCommenter.NOT_COMMENTED.contains(type)) {
            return json;
        }
        if (json.startsWith("{")) {
            return json;
        }
        var builder = new StringBuilder();
        builder.append("{").append(AonCommenter.NEWLINE);
        var lines = Lists.newArrayList(json.trim().split("\\R"));
        var index = 0;
        for (var line : lines) {
            if (!line.trim().isEmpty() && !line.startsWith("#")) {
                var nextIsElement = AonCommenter.nextIsElement(lines, index);
                if (line.startsWith(" ") || !line.contains(":")) {
                    builder.append("  ").append(line);
                } else {
                    var split = line.split(":");
                    builder.append("  ").append("\"").append(split[0]).append("\":").append(TextUtils.concat(1, ":", split));
                }
                if (nextIsElement && !line.endsWith("[") && !line.endsWith("{")) {
                    builder.append(",");
                }
                builder.append(AonCommenter.NEWLINE);
            }
            index++;
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * Checks if the next line that is not empty and not a comment holds another element, and does not close an array
     * or an object.
     *
     * @param lines the AON lines
     * @param index the index of the current line
     * @return {@code true} if a comma must follow the current line
     */
    private static boolean nextIsElement(List<String> lines, int index) {
        if (index + 1 >= lines.size()) {
            return false;
        }
        for (var i = index + 1; i < lines.size(); i++) {
            var line = lines.get(i).trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                return !line.equalsIgnoreCase("]") && !line.equalsIgnoreCase("}");
            }
        }
        return false;
    }

    /**
     * Converts a camel case Java field name to a snake case JSON field name.
     *
     * @param field the Java field name, for example {@code maxPlayerCount}
     * @return the JSON field name, for example {@code max_player_count}
     */
    private static String javaToJsonField(String field) {
        while (true) {
            var matcher = JAVA_TO_JSON_FIELD.matcher(field);
            if (!matcher.find()) {
                break;
            }
            field = matcher.replaceAll(result -> result.group(1) + "_" + result.group(2).toLowerCase(Locale.ROOT));
        }
        return field;
    }

    /**
     * Converts a snake case JSON field name to a camel case Java field name.
     *
     * @param field the JSON field name, for example {@code max_player_count}
     * @return the Java field name, for example {@code maxPlayerCount}
     */
    private static String jsonToJavaField(String field) {
        while (true) {
            var matcher = JSON_TO_JAVA_FIELD.matcher(field);
            if (!matcher.find()) {
                break;
            }
            field = matcher.replaceAll(result -> result.group(2).toUpperCase(Locale.ROOT));
        }
        return field;
    }

    /**
     * Holds the {@link AonComment} comments of a configuration class and of its fields.
     */
    private static final class Info {

        private final CommentHolder root;
        private final Map<String, Optional<CommentHolder>> comments = new HashMap<>();

        /**
         * Reads the comment of the class and the comments of its declared fields.
         *
         * @param root the comment on the configuration class
         * @param type the configuration class
         */
        private Info(AonComment root, Class<? extends Configuration> type) {
            this.root = new CommentHolder(root);
            for (var field : type.getDeclaredFields()) {
                var comment = field.getAnnotation(AonComment.class);
                if (comment != null) {
                    this.putComment(field.getName(), comment);
                }
            }
        }

        /**
         * @return the comment on the configuration class
         */
        public CommentHolder root() {
            return this.root;
        }

        /**
         * @return the field comments, keyed by JSON field name
         */
        public Map<String, Optional<CommentHolder>> comments() {
            return this.comments;
        }

        /**
         * Stores the comment of a field under its JSON field name.
         *
         * @param field the Java field name
         * @param comment the comment on the field
         */
        public void putComment(String field, AonComment comment) {
            this.comments.put(AonCommenter.javaToJsonField(field), Optional.of(new CommentHolder(comment)));
        }

        /**
         * Gets the comment of a field.
         *
         * @param field the JSON field name
         * @return the comment, or an empty optional if the field has no comment
         */
        public Optional<CommentHolder> getComment(String field) {
            return this.comments.getOrDefault(field, Optional.empty());
        }

        /**
         * Converts JSON to AON. Removes the outer braces, the quotes around top-level keys and the trailing commas, and
         * adds the class comment and the field comments.
         *
         * @param json the JSON of the configuration
         * @return the AON text
         */
        public String comment(String json) {
            var builder = new StringBuilder();
            this.root.apply(builder);
            var lines = json.split(AonCommenter.NEWLINE);
            for (var line : lines) {
                if (line.startsWith("  ")) {
                    line = line.substring(2);
                    if (line.startsWith("  ") || !line.contains(":")) {
                        if (line.endsWith(",")) {
                            line = line.substring(0, line.length() - 1);
                        }
                        builder.append(line);
                    } else {
                        var split = line.split(":");
                        var last = split[split.length - 1];
                        if (last.endsWith(",")) {
                            split[split.length - 1] = last.substring(0, last.length() - 1);
                        }
                        var field = split[0].replace("\"", "");
                        if (!builder.isEmpty()) {
                            builder.append(AonCommenter.NEWLINE);
                        }
                        this.getComment(field).ifPresent(comment -> comment.apply(builder));
                        builder.append(field).append(":").append(TextUtils.concat(1, ":", split));
                    }
                    builder.append(AonCommenter.NEWLINE);
                }
            }
            return builder.toString();
        }

        /**
         * Holds the lines and the indents of one {@link AonComment}.
         */
        private static final class CommentHolder {

            private final String[] lines;
            private final int[] indents;

            /**
             * Copies the lines and the indents of the comment.
             *
             * @param comment the comment to hold
             */
            private CommentHolder(AonComment comment) {
                this.lines = comment.value();
                this.indents = comment.indents();
            }

            /**
             * Appends each line, prefixed with {@code #} characters. The indent of the line gives the number of
             * {@code #} characters, and the default is {@code 1}. An empty line becomes a blank line.
             *
             * @param builder the builder to append to
             */
            private void apply(StringBuilder builder) {
                for (var i = 0; i < this.lines.length; i++) {
                    var indent = this.indents.length > i ? this.indents[i] : 1;
                    var line = this.lines[i];
                    if (!line.isEmpty()) {
                        builder.repeat("#", Math.max(0, indent)).append(" ").append(line);
                    }
                    builder.append(AonCommenter.NEWLINE);
                }
            }
        }
    }
}
