package de.waterdu.atlantis.gametest;

import de.waterdu.atlantis.mixin.network.MixinServerCommonPacketListenerImpl;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Access to the embedded channel behind a GameTest mock player's connection.
 */
public final class GameTestConnections {

    private GameTestConnections() {}

    public static EmbeddedChannel channel(ServerPlayer player) {
        var connection = ((MixinServerCommonPacketListenerImpl) (Object) player.connection).atlantis$getConnection();
        try {
            var field = Connection.class.getDeclaredField("channel");
            field.setAccessible(true);
            return (EmbeddedChannel) field.get(connection);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not read the mock player's channel", e);
        }
    }

    /** Drains the packets written to the mock player since the last call. */
    public static List<Packet<?>> sent(ServerPlayer player) {
        var channel = channel(player);
        var packets = new ArrayList<Packet<?>>();
        Object message;
        while ((message = channel.readOutbound()) != null) {
            if (message instanceof Packet<?> packet) {
                packets.add(packet);
            }
        }
        return packets;
    }
}
