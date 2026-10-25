package de.waterdu.atlantis.mod.impl.common.event.player;

import com.mojang.brigadier.ParseResults;
import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.Nullable;

/**
 * Fires before a parsed command runs, from any source. Cancelling it stops the command silently.
 */
public class CommandEventPayload implements EventPayload, Cancellable {

    private final ParseResults<CommandSourceStack> parse;
    private final String command;
    private boolean cancelled = false;

    public CommandEventPayload(ParseResults<CommandSourceStack> parse, String command) {
        this.parse = parse;
        this.command = command;
    }

    @Override
    public Key<?> type() {
        return UEvent.COMMAND;
    }

    public CommandSourceStack source() {
        return this.parse.getContext().getSource();
    }

    public ParseResults<CommandSourceStack> parse() {
        return this.parse;
    }

    public String command() {
        return this.command;
    }

    /**
     * The first literal of the command as it was typed, such as {@code tp} for {@code /tp @s ~ ~ ~}.
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

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }
}
