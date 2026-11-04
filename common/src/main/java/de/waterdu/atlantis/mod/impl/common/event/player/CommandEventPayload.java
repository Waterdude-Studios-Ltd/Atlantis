package de.waterdu.atlantis.mod.impl.common.event.player;

import com.mojang.brigadier.ParseResults;
import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.Nullable;

/**
 * Fires before a parsed command from any source runs. If a listener cancels it, the command stops silently.
 */
public class CommandEventPayload implements EventPayload, Cancellable {

    private final ParseResults<CommandSourceStack> parse;
    private final String command;
    private boolean cancelled = false;

    /**
     * Creates the payload for a parsed command.
     *
     * @param parse the parse results of the command
     * @param command the command string
     */
    public CommandEventPayload(ParseResults<CommandSourceStack> parse, String command) {
        this.parse = parse;
        this.command = command;
    }

    /**
     * @return {@link UEvent#COMMAND}
     */
    @Override
    public Key<?> type() {
        return UEvent.COMMAND;
    }

    /**
     * @return the source that runs the command, from the parse context
     */
    public CommandSourceStack source() {
        return this.parse.getContext().getSource();
    }

    /**
     * @return the parse results of the command
     */
    public ParseResults<CommandSourceStack> parse() {
        return this.parse;
    }

    /**
     * @return the command string
     */
    public String command() {
        return this.command;
    }

    /**
     * The first literal of the command, as typed, for example {@code tp} for {@code /tp @s ~ ~ ~}.
     */
    public @Nullable String rootLiteral() {
        var nodes = this.parse.getContext().getNodes();
        if (!nodes.isEmpty()) {
            return nodes.getFirst().getNode().getName();
        }
        var trimmed = this.command.startsWith("/") ? this.command.substring(1) : this.command;
        var space = trimmed.indexOf(' ');
        var root = space == -1 ? trimmed : trimmed.substring(0, space);
        return root.isEmpty() ? null : root;
    }

    /**
     * Cancels the event. The command then stops silently.
     */
    @Override
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * @return {@code true} if the event is cancelled
     */
    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
