package de.waterdu.atlantis.util.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CommandRegistryTest {

    @Test
    void replaysRegistrantsIntoEveryDispatcher() throws CommandSyntaxException {
        var name = "atlantis_test_" + UUID.randomUUID().toString().substring(0, 8);
        var executions = new AtomicInteger();
        CommandRegistry.register(dispatcher -> dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal(name).executes(context -> executions.incrementAndGet())));
        var first = new CommandDispatcher<CommandSourceStack>();
        var second = new CommandDispatcher<CommandSourceStack>();

        CommandRegistry.registerAll(first);
        CommandRegistry.registerAll(second);

        assertThat(first.getRoot().getChild(name)).isNotNull();
        assertThat(second.getRoot().getChild(name)).isNotNull();
        assertThat(first.execute(name, null)).isEqualTo(1);
        assertThat(second.execute(name, null)).isEqualTo(2);
    }

    @Test
    void registrantsRunInRegistrationOrder() {
        var order = new StringBuilder();
        CommandRegistry.register(dispatcher -> order.append("a"));
        CommandRegistry.register(dispatcher -> order.append("b"));

        CommandRegistry.registerAll(new CommandDispatcher<>());

        assertThat(order.toString()).endsWith("ab");
    }
}
