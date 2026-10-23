package de.waterdu.atlantis.test;

import net.minecraft.server.MinecraftServer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Mock servers for code that only needs {@code ServerUtils.server()} to exist. Use with {@link MinecraftBootstrapped}.
 */
public final class MockServers {

    private MockServers() {}

    /** A server whose {@code execute} runs tasks immediately on the calling thread. */
    public static MinecraftServer executingInline() {
        var server = mock(MinecraftServer.class);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(server).execute(any());
        return server;
    }
}
