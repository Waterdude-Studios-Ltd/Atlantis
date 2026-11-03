package de.waterdu.atlantis.util.server;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.collection.UnaryMap;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.text.Text;
import de.waterdu.atlantis.util.text.TextUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Holds a command line that runs as the server. It can contain {@code %}-placeholders and entity selectors.
 *
 * @param value the command, without the {@code /}
 */
public record Command(String value) {

    public static final char PLACEHOLDER_CHAR = '%';
    public static final char SELECTOR_CHAR = '@';

    public static final String PLAYER_PLACEHOLDER = PLACEHOLDER_CHAR + "p" + PLACEHOLDER_CHAR;
    public static final String ALTERNATE_PLAYER_PLACEHOLDER = PLACEHOLDER_CHAR + "player" + PLACEHOLDER_CHAR;
    public static final String X_PLACEHOLDER = PLACEHOLDER_CHAR + "x" + PLACEHOLDER_CHAR;
    public static final String Y_PLACEHOLDER = PLACEHOLDER_CHAR + "y" + PLACEHOLDER_CHAR;
    public static final String Z_PLACEHOLDER = PLACEHOLDER_CHAR + "z" + PLACEHOLDER_CHAR;
    public static final String DIMENSION_PLACEHOLDER = PLACEHOLDER_CHAR + "dim" + PLACEHOLDER_CHAR;

    /**
     * Creates a command from a string.
     *
     * @param command the command line
     * @return the command
     */
    public static Command of(String command) {
        return new Command(command);
    }

    /**
     * Creates a command from the raw string of a text.
     *
     * @param text the text that holds the command line
     * @return the command
     */
    public static Command of(Text text) {
        return new Command(text.get());
    }

    /**
     * Creates a context to fill the placeholders of this command and run it.
     *
     * @return a new context for this command
     */
    public CommandContext prepareContext() {
        return new CommandContext(this);
    }

    /**
     * Collects the placeholder values and the selector source for a command, then runs it or defers it.
     */
    public static class CommandContext {

        private final Command command;
        private CommandSourceStack source;
        private String player;
        private LevelPosition position;
        private final UnaryMap<String> placeholders = UnaryMap.create();

        /**
         * Creates a context for the command.
         *
         * @param command the command that {@link #execute()} and {@link #defer()} use, or {@code null} for none
         */
        private CommandContext(Command command) {
            this.command = command;
        }

        /**
         * A context without a command. Use {@link #execute(Command)} or {@link #defer(Command)} with it.
         */
        public static CommandContext empty() {
            return new CommandContext(null);
        }

        /**
         * Fills {@code %p%} and {@code %player%} with the name of the player. Does nothing when the player is not found.
         */
        public CommandContext player(Object player) {
            PlayerReference.getSafely(player).ifPresent(reference -> this.player = reference.name());
            return this;
        }

        /**
         * Fills {@code %p%} and {@code %player%} with the name.
         *
         * @param player the name of the player
         * @return this context
         */
        public CommandContext player(String player) {
            this.player = player;
            return this;
        }

        /**
         * Uses the server as the source for the entity selectors in the command.
         *
         * @return this context
         */
        public CommandContext serverSource() {
            return this.source(null);
        }

        /**
         * Only the entity selectors in the command use this source. The command always runs as the server.
         *
         * @param player the player to use as the source. null uses the server.
         */
        public CommandContext source(ServerPlayer player) {
            this.source = player == null ? ServerUtils.server().createCommandSourceStack() : player.createCommandSourceStack();
            return this;
        }

        /**
         * Fills the position placeholders with the position of the entity.
         *
         * @param entity the entity to get the position from
         * @return this context
         */
        public CommandContext position(Entity entity) {
            return this.position(LevelPosition.of(entity));
        }

        /**
         * Fills {@code %x%}, {@code %y%} and {@code %z%} with the coordinates truncated to integers,
         * and {@code %dim%} with the level id.
         */
        public CommandContext position(LevelPosition position) {
            this.position = position;
            return this;
        }

        /**
         * Fills {@code %key%} with the name of the player. Does nothing when the player is not found.
         */
        public CommandContext extraPlayer(String key, Object player) {
            PlayerReference.getSafely(player).ifPresent(reference -> this.placeholders.put(key, reference.name()));
            return this;
        }

        /**
         * @param key the placeholder name without the {@code %} characters
         */
        public CommandContext placeholder(String key, Object value) {
            this.placeholders.put(key, String.valueOf(value));
            return this;
        }

        /**
         * Runs the command of this context with {@link #execute(Command)}. If the context has no command, it does
         * nothing.
         */
        public void execute() {
            this.execute(this.command);
        }

        /**
         * Fills the placeholders, then runs the command as the server. Errors are logged.
         * If a word after a space starts with {@code @}, it is an entity selector. Then the command runs once for each
         * entity that matches, with the player name or entity UUID in place of the selector.
         */
        public void execute(Command command) {
            if (command != null) {
                this.compile(command).execute();
            }
        }

        /**
         * Prepares the command of this context with {@link #defer(Command)}.
         *
         * @return the commands to run later, or an empty executable if the context has no command
         */
        public Executable defer() {
            return this.defer(this.command);
        }

        /**
         * {@link #execute(Command)}, but returns the commands to run later. The placeholders and selectors are resolved now.
         */
        public Executable defer(Command command) {
            if (command != null) {
                return this.compile(command);
            }
            return Executable.EMPTY;
        }

        /**
         * Fills the placeholders of the command and resolves the first entity selector that parses. If no source is
         * set, it sets the server as the source.
         *
         * @param command the command to prepare
         * @return one command for each selected entity, or the filled command if no selector parses
         */
        private Executable compile(Command command) {
            String value = command.value;
            Executable executable = new Executable();

            if (this.source == null) {
                this.serverSource();
            }

            if (this.player != null) {
                value = value.replace(PLAYER_PLACEHOLDER, this.player)
                        .replace(ALTERNATE_PLAYER_PLACEHOLDER, this.player);
            }

            if (this.position != null) {
                value = value.replace(X_PLACEHOLDER, String.valueOf((int) this.position.x()))
                        .replace(Y_PLACEHOLDER, String.valueOf((int) this.position.y()))
                        .replace(Z_PLACEHOLDER, String.valueOf((int) this.position.z()))
                        .replace(DIMENSION_PLACEHOLDER, this.position.levelKey().location().toString());
            }

            for (Map.Entry<String, String> entry : this.placeholders.entrySet()) {
                value = value.replace(PLACEHOLDER_CHAR + entry.getKey() + PLACEHOLDER_CHAR, entry.getValue());
            }

            var selected = false;
            if (value.contains(" " + SELECTOR_CHAR)) {
                var words = value.split(" ");
                for (int i = 0; i < words.length; i++) {
                    var word = words[i];
                    if (word.startsWith(String.valueOf(SELECTOR_CHAR))) {
                        try {
                            var selector = new EntitySelectorParser(new StringReader(word), true).parse();
                            var entities = selector.findEntities(this.source);
                            for (var entity : entities) {
                                if (entity instanceof Player) {
                                    executable.add(TextUtils.concatExcept(i, entity.getName().getString(), " ", words));
                                } else {
                                    executable.add(TextUtils.concatExcept(i, entity.getStringUUID(), " ", words));
                                }
                            }
                            selected = true;
                            break;
                        } catch (CommandSyntaxException ignored) {
                            // ignored
                        }
                    }
                }
            }

            if (!selected) {
                executable.add(value);
            }

            return executable;
        }
    }

    /**
     * Holds prepared command lines to run as the server.
     */
    public static class Executable {

        private static final Executable EMPTY = new Executable();

        private final List<String> commands = new ArrayList<>();

        /**
         * Creates an executable without commands.
         */
        private Executable() {
            // noop
        }

        /**
         * Adds a command line to run.
         *
         * @param command the command line
         */
        public void add(String command) {
            this.commands.add(command);
        }

        /**
         * {@link #execute(Object...)}, but stops and throws at the first command that fails.
         */
        public void executeUnsafe(Object... args) throws CommandSyntaxException {
            for (var command : this.commands) {
                ServerUtils.performCommand(command, args);
            }
        }

        /**
         * Runs each command as the server. Logs the commands that fail.
         *
         * @param args the values for {@code %1}, {@code %2} and so on in each command
         */
        public void execute(Object... args) {
            for (var command : this.commands) {
                try {
                    ServerUtils.performCommand(command, args);
                } catch (CommandSyntaxException e) {
                    AtlantisLogger.error("Failed to execute command '{}': {}", command, e.getMessage());
                }
            }
        }
    }

    /**
     * Reads and writes a {@link Command} as a JSON string.
     */
    public static class TypeAdapter implements JsonDeserializer<Command>, JsonSerializer<Command> {

        /**
         * Creates a command from the string value of the JSON element.
         *
         * @param json the JSON element
         * @param typeOfT the type to read
         * @param context the deserialization context
         * @return the command
         */
        @Override
        public Command deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Command.of(json.getAsString());
        }

        /**
         * Writes the command line as a JSON string.
         *
         * @param src the command to write
         * @param typeOfSrc the type to write
         * @param context the serialization context
         * @return the JSON string
         */
        @Override
        public JsonElement serialize(Command src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.value);
        }
    }
}
