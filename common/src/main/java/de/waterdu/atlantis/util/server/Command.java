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

public record Command(String value) {

    public static final char PLACEHOLDER_CHAR = '%';
    public static final char SELECTOR_CHAR = '@';

    public static final String PLAYER_PLACEHOLDER = PLACEHOLDER_CHAR + "p" + PLACEHOLDER_CHAR;
    public static final String ALTERNATE_PLAYER_PLACEHOLDER = PLACEHOLDER_CHAR + "player" + PLACEHOLDER_CHAR;
    public static final String X_PLACEHOLDER = PLACEHOLDER_CHAR + "x" + PLACEHOLDER_CHAR;
    public static final String Y_PLACEHOLDER = PLACEHOLDER_CHAR + "y" + PLACEHOLDER_CHAR;
    public static final String Z_PLACEHOLDER = PLACEHOLDER_CHAR + "z" + PLACEHOLDER_CHAR;
    public static final String DIMENSION_PLACEHOLDER = PLACEHOLDER_CHAR + "dim" + PLACEHOLDER_CHAR;

    public static Command of(String command) {
        return new Command(command);
    }

    public static Command of(Text text) {
        return new Command(text.get());
    }

    public CommandContext prepareContext() {
        return new CommandContext(this);
    }

    public static class CommandContext {

        private final Command command;
        private CommandSourceStack source;
        private String player;
        private LevelPosition position;
        private final UnaryMap<String> placeholders = UnaryMap.create();

        private CommandContext(Command command) {
            this.command = command;
        }

        public static CommandContext empty() {
            return new CommandContext(null);
        }

        public CommandContext player(Object player) {
            PlayerReference.getSafely(player).ifPresent(reference -> this.player = reference.name());
            return this;
        }

        public CommandContext player(String player) {
            this.player = player;
            return this;
        }

        public CommandContext serverSource() {
            return this.source(null);
        }

        public CommandContext source(ServerPlayer player) {
            this.source = player == null ? ServerUtils.server().createCommandSourceStack() : player.createCommandSourceStack();
            return this;
        }

        public CommandContext position(Entity entity) {
            return this.position(LevelPosition.of(entity));
        }

        public CommandContext position(LevelPosition position) {
            this.position = position;
            return this;
        }

        public CommandContext extraPlayer(String key, Object player) {
            PlayerReference.getSafely(player).ifPresent(reference -> this.placeholders.put(key, reference.name()));
            return this;
        }

        public CommandContext placeholder(String key, Object value) {
            this.placeholders.put(key, String.valueOf(value));
            return this;
        }

        public void execute() {
            this.execute(this.command);
        }

        public void execute(Command command) {
            if (command != null) {
                this.compile(command).execute();
            }
        }

        public Executable defer() {
            return this.defer(this.command);
        }

        public Executable defer(Command command) {
            if (command != null) {
                return this.compile(command);
            }
            return Executable.EMPTY;
        }

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

    public static class Executable {

        private static final Executable EMPTY = new Executable();

        private final List<String> commands = new ArrayList<>();

        private Executable() {
            // noop
        }

        public void add(String command) {
            this.commands.add(command);
        }

        public void executeUnsafe(Object... args) throws CommandSyntaxException {
            for (var command : this.commands) {
                ServerUtils.performCommand(command, args);
            }
        }

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

    public static class TypeAdapter implements JsonDeserializer<Command>, JsonSerializer<Command> {

        @Override
        public Command deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Command.of(json.getAsString());
        }

        @Override
        public JsonElement serialize(Command src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.value);
        }
    }
}
