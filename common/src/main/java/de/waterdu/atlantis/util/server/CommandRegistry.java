package de.waterdu.atlantis.util.server;

import com.mojang.brigadier.CommandDispatcher;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class CommandRegistry {

    private static final List<Consumer<CommandDispatcher<CommandSourceStack>>> REGISTRANTS = new CopyOnWriteArrayList<>();

    private CommandRegistry() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * The loaders run the registrant for each dispatcher that they build, so it can run more than once.
     */
    public static void register(Consumer<CommandDispatcher<CommandSourceStack>> registrant) {
        REGISTRANTS.add(registrant);
    }

    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (var registrant : REGISTRANTS) {
            registrant.accept(dispatcher);
        }
    }
}
