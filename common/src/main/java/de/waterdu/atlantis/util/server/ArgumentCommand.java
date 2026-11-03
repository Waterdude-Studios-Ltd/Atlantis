package de.waterdu.atlantis.util.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.text.MessageUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.InteractionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * A command that takes its arguments as a list of words. Use it for commands that parse their own arguments instead
 * of describing them to Brigadier. Atlantis registers it under {@link #name()} and every {@link #aliases()}. Atlantis
 * reads these names again after the server has started. This gets the names from configs, because configs load after
 * the first build of the commands.
 */
public abstract class ArgumentCommand {

    private static final String ARGUMENTS = "arguments";

    /**
     * Registers the command with every dispatcher that Atlantis builds, and with the live dispatcher after the server
     * has started.
     */
    public void register() {
        CommandRegistry.register(this::register);
        Mods.modLoader().event(UEvent.SERVER_STARTED).listen(payload -> {
            var server = payload.server();
            if (server != null) {
                this.register(server.getCommands().getDispatcher());
                server.getPlayerList().getPlayers().forEach(player -> server.getCommands().sendCommands(player));
            }
            return InteractionResult.PASS;
        });
    }

    /**
     * Registers the command under its name and each distinct, non-blank alias. Each name runs the command without
     * arguments, or with the rest of the input split into words.
     *
     * @param dispatcher the dispatcher to register the command with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var names = new ArrayList<String>();
        names.add(this.name());
        for (var alias : this.aliases()) {
            if (!alias.isBlank() && !names.contains(alias.trim())) {
                names.add(alias.trim());
            }
        }
        for (var name : names) {
            dispatcher.register(Commands.literal(name)
                    .requires(this.requirement())
                    .executes(context -> this.run(context, new String[0]))
                    .then(Commands.argument(ARGUMENTS, StringArgumentType.greedyString())
                            .suggests(this::suggest)
                            .executes(context -> this.run(context, split(StringArgumentType.getString(context, ARGUMENTS))))));
        }
    }

    /**
     * @return the main name of the command, without the {@code /}
     */
    public abstract String name();

    /**
     * @return the other names of the command. The default has none.
     */
    public List<String> aliases() {
        return List.of();
    }

    /**
     * @return the test that a source must pass to use the command. The default lets every source use it.
     */
    public Predicate<CommandSourceStack> requirement() {
        return source -> true;
    }

    /**
     * Exceptions are logged and do not reach the source.
     *
     * @param args the words after the command name, split at whitespace. Empty when there are none.
     */
    public abstract void execute(CommandContext<CommandSourceStack> context, String... args);

    /**
     * Gets the possible values for the last argument. Only the values that start with the typed text are returned.
     */
    public List<String> suggestions(CommandContext<CommandSourceStack> context, String... args) {
        return List.of();
    }

    /**
     * Checks if the argument is equal to one of the options, ignoring case.
     *
     * @param arg the argument to check
     * @param options the accepted values
     * @return {@code true} if the argument matches an option
     */
    protected boolean match(String arg, String... options) {
        for (var option : options) {
            if (option.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends the message to the source with {@link MessageUtils#send}.
     *
     * @param source the source to send the message to
     * @param message the message to send
     */
    protected void send(CommandSourceStack source, Object message) {
        MessageUtils.send(message, source);
    }

    /**
     * Runs {@link #execute} and logs an exception if it throws one.
     *
     * @param context the context of the command
     * @param args the words after the command name
     * @return {@code 1} if the command succeeds, or {@code 0} if it throws
     */
    private int run(CommandContext<CommandSourceStack> context, String[] args) {
        try {
            this.execute(context, args);
            return 1;
        } catch (Exception e) {
            AtlantisLogger.error("Command /{} failed: {}", this.name(), e.getMessage());
            AtlantisLogger.debugBuilder().withThrowable(e).log();
            return 0;
        }
    }

    /**
     * Suggests the values from {@link #suggestions} that start with the last typed word, ignoring case. If the input is
     * empty or ends with a space, the last word is empty.
     *
     * @param context the context of the command
     * @param builder the builder for the suggestions
     * @return the future of the suggestions
     */
    private CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        var input = builder.getRemaining();
        var args = input.endsWith(" ") || input.isEmpty() ? append(split(input), "") : split(input);
        var last = args[args.length - 1].toLowerCase(Locale.ROOT);
        var offset = builder.createOffset(builder.getStart() + input.length() - last.length());
        for (var suggestion : this.suggestions(context, args)) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(last)) {
                offset.suggest(suggestion);
            }
        }
        return offset.buildFuture();
    }

    /**
     * Splits the input into words at whitespace.
     *
     * @param input the text to split
     * @return the words, or an empty array if the input is blank
     */
    private static String[] split(String input) {
        var trimmed = input.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    /**
     * Creates a copy of the array with one more element at the end.
     *
     * @param args the array to copy
     * @param arg the element to add
     * @return the new array
     */
    private static String[] append(String[] args, String arg) {
        var appended = new String[args.length + 1];
        System.arraycopy(args, 0, appended, 0, args.length);
        appended[args.length] = arg;
        return appended;
    }
}
