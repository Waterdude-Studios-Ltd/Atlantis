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
 * A command that takes its arguments as a list of words, for commands that parse their own arguments rather than
 * describing them to Brigadier. It is registered under {@link #name()} and every {@link #aliases()}, which are read
 * again once the server has started so that names from configs, which load after commands are first built, are
 * picked up.
 */
public abstract class ArgumentCommand {

    private static final String ARGUMENTS = "arguments";

    /**
     * Registers the command with every dispatcher Atlantis builds, and with the live dispatcher once the server has
     * started.
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

    public abstract String name();

    public List<String> aliases() {
        return List.of();
    }

    public Predicate<CommandSourceStack> requirement() {
        return source -> true;
    }

    public abstract void execute(CommandContext<CommandSourceStack> context, String... args);

    /**
     * Gets the possible values for the last argument. They are filtered to those starting with what has been typed.
     */
    public List<String> suggestions(CommandContext<CommandSourceStack> context, String... args) {
        return List.of();
    }

    protected boolean match(String arg, String... options) {
        for (var option : options) {
            if (option.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    protected void send(CommandSourceStack source, Object message) {
        MessageUtils.send(message, source);
    }

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

    private static String[] split(String input) {
        var trimmed = input.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    private static String[] append(String[] args, String arg) {
        var appended = new String[args.length + 1];
        System.arraycopy(args, 0, appended, 0, args.length);
        appended[args.length] = arg;
        return appended;
    }
}
