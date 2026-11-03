package de.waterdu.atlantis.util.server;

import com.mojang.brigadier.CommandDispatcher;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Holds the command registrants that the loaders run on each command dispatcher.
 */
public final class CommandRegistry {

    private static final List<Consumer<CommandDispatcher<CommandSourceStack>>> REGISTRANTS = new CopyOnWriteArrayList<>();

    /**
     * Always throws, because this class only has static methods.
     */
    private CommandRegistry() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * The loaders run the registrant for each dispatcher that they build, so it can run more than once.
     */
    public static void register(Consumer<CommandDispatcher<CommandSourceStack>> registrant) {
        REGISTRANTS.add(registrant);
    }

    /**
     * Runs every registrant on the dispatcher, in the order they were registered.
     *
     * @param dispatcher the dispatcher to register the commands with
     */
    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (var registrant : REGISTRANTS) {
            registrant.accept(dispatcher);
        }
    }
}
