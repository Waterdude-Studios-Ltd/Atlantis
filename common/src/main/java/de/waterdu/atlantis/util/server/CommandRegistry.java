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

    public static void register(Consumer<CommandDispatcher<CommandSourceStack>> registrant) {
        REGISTRANTS.add(registrant);
    }

    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (var registrant : REGISTRANTS) {
            registrant.accept(dispatcher);
        }
    }
}
